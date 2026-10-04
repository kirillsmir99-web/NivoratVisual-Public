using System;
using System.Runtime.InteropServices;
using System.Threading;

namespace NvMedia {
    // WASAPI render loopback reads the output device, never the microphone.
    internal static class AudioSpectrum {
        private static readonly object Gate = new object();
        private static float[] bands = new float[12];
        private static long sampled;
        private static volatile bool running;
        private static volatile bool available;
        public static bool Available { get { return available; } }
        public static float[] Snapshot() {
            lock (Gate) return DateTime.UtcNow.Ticks-sampled > TimeSpan.TicksPerSecond ? new float[12] : (float[])bands.Clone();
        }
        public static void Start() {
            running=true;
            var worker=new Thread(Run) { IsBackground=true, Name="NV output spectrum" };
            worker.SetApartmentState(ApartmentState.MTA);worker.Start();
        }
        public static void Stop() { running=false; }
        private static void Run() {
            while (running) {
                try { Capture(); } catch (Exception) { }
                if (running) Thread.Sleep(1000);
            }
        }
        private static void Capture() {
            IMMDeviceEnumerator enumerator=null; IMMDevice device=null; IAudioClient client=null; IAudioCaptureClient capture=null;
            IntPtr format=IntPtr.Zero;
            try {
                enumerator=(IMMDeviceEnumerator)new MMDeviceEnumerator();
                Marshal.ThrowExceptionForHR(enumerator.GetDefaultAudioEndpoint(0,0,out device));
                Guid clientId=typeof(IAudioClient).GUID;object obj;
                Marshal.ThrowExceptionForHR(device.Activate(ref clientId,23,IntPtr.Zero,out obj));client=(IAudioClient)obj;
                Marshal.ThrowExceptionForHR(client.GetMixFormat(out format));
                int tag=Marshal.ReadInt16(format,0)&65535,channels=Marshal.ReadInt16(format,2)&65535;
                int rate=Marshal.ReadInt32(format,4),stride=Marshal.ReadInt16(format,12)&65535,bits=Marshal.ReadInt16(format,14)&65535;
                if(tag==65534) tag=Marshal.ReadInt16(format,24)&65535;
                if(channels<1 || stride<1 || !(tag==3&&bits==32 || tag==1&&(bits==16||bits==32))) return;
                Guid session=Guid.Empty;
                Marshal.ThrowExceptionForHR(client.Initialize(0,0x20000,1000000,0,format,ref session));
                Guid captureId=typeof(IAudioCaptureClient).GUID;
                Marshal.ThrowExceptionForHR(client.GetService(ref captureId,out obj));capture=(IAudioCaptureClient)obj;
                Marshal.ThrowExceptionForHR(client.Start());
                available=true;
                var window=new double[2048];int count=0;
                while(running) {
                    uint frames;Marshal.ThrowExceptionForHR(capture.GetNextPacketSize(out frames));
                    while(frames>0) {
                        IntPtr data;uint flags;ulong position,clock;
                        Marshal.ThrowExceptionForHR(capture.GetBuffer(out data,out frames,out flags,out position,out clock));
                        try {
                            byte[] bytes=new byte[checked((int)frames*stride)];
                            if((flags&2)==0) Marshal.Copy(data,bytes,0,bytes.Length);
                            for(int f=0;f<frames;f++) {
                                double sample=0;
                                for(int c=0;c<channels;c++) {
                                    int offset=f*stride+c*(bits/8);
                                    sample+=tag==3?BitConverter.ToSingle(bytes,offset):bits==16?BitConverter.ToInt16(bytes,offset)/32768.0:BitConverter.ToInt32(bytes,offset)/2147483648.0;
                                }
                                window[count++]=sample/channels;
                                if(count==window.Length) { Analyse(window,rate);count=0; }
                            }
                        } finally { capture.ReleaseBuffer(frames); }
                        Marshal.ThrowExceptionForHR(capture.GetNextPacketSize(out frames));
                    }
                    Thread.Sleep(10);
                }
            } finally {
                available=false;
                if(client!=null) { try { client.Stop(); } catch(Exception) {} }
                if(format!=IntPtr.Zero) Marshal.FreeCoTaskMem(format);
                foreach(object obj in new object[]{capture,client,device,enumerator}) if(obj!=null && Marshal.IsComObject(obj)) Marshal.ReleaseComObject(obj);
            }
        }
        private static void Analyse(double[] samples,int rate) {
            float[] result=new float[12];
            int n=samples.Length;
            double[] real=new double[n],imag=new double[n];
            for(int i=0,j=0;i<n;i++) {
                real[j]=samples[i]*(.5-.5*Math.Cos(2*Math.PI*i/(n-1)));
                int bit=n>>1;while((j&bit)!=0) {j^=bit;bit>>=1;}j^=bit;
            }
            for(int length=2;length<=n;length<<=1) {
                double angle=-2*Math.PI/length,stepR=Math.Cos(angle),stepI=Math.Sin(angle);
                for(int start=0;start<n;start+=length) {
                    double wr=1,wi=0;
                    for(int k=0;k<length/2;k++) {
                        int a=start+k,b=a+length/2;
                        double tr=wr*real[b]-wi*imag[b],ti=wr*imag[b]+wi*real[b];
                        real[b]=real[a]-tr;imag[b]=imag[a]-ti;real[a]+=tr;imag[a]+=ti;
                        double next=wr*stepR-wi*stepI;wi=wr*stepI+wi*stepR;wr=next;
                    }
                }
            }
            // Sum neighbouring bins into logarithmic bands instead of sampling one tone.
            for(int b=0;b<12;b++) {
                double low=40*Math.Pow(400,b/12.0),high=40*Math.Pow(400,(b+1)/12.0),energy=0;
                int first=Math.Max(1,(int)(low*samples.Length/rate)),last=Math.Min(samples.Length/2,(int)Math.Ceiling(high*samples.Length/rate));
                for(int k=first;k<=last;k++) {
                    energy+=(real[k]*real[k]+imag[k]*imag[k])/(n*n);
                }
                double amplitude=Math.Sqrt(energy)*4;
                result[b]=(float)Math.Max(0,Math.Min(1,(20*Math.Log10(Math.Max(1e-6,amplitude))+60)/60));
            }
            lock(Gate) { for(int b=0;b<12;b++) bands[b]=Math.Max(result[b],bands[b]*.78f);sampled=DateTime.UtcNow.Ticks; }
        }
        [ComImport,Guid("BCDE0395-E52F-467C-8E3D-C4579291692E")] private class MMDeviceEnumerator {}
        [ComImport,Guid("A95664D2-9614-4F35-A746-DE8DB63617E6"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)] private interface IMMDeviceEnumerator {
            [PreserveSig] int EnumAudioEndpoints(int flow,int mask,out IntPtr collection);
            [PreserveSig] int GetDefaultAudioEndpoint(int flow,int role,out IMMDevice device);
        }
        [ComImport,Guid("D666063F-1587-4E43-81F1-B948E807363F"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)] private interface IMMDevice {
            [PreserveSig] int Activate(ref Guid id,int context,IntPtr activation,[MarshalAs(UnmanagedType.IUnknown)] out object value);
        }
        [ComImport,Guid("1CB9AD4C-DBFA-4C32-B178-C2F568A703B2"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)] private interface IAudioClient {
            [PreserveSig] int Initialize(int mode,int flags,long duration,long period,IntPtr format,ref Guid session);
            [PreserveSig] int GetBufferSize(out uint size);
            [PreserveSig] int GetStreamLatency(out long latency);
            [PreserveSig] int GetCurrentPadding(out uint padding);
            [PreserveSig] int IsFormatSupported(int mode,IntPtr format,out IntPtr closest);
            [PreserveSig] int GetMixFormat(out IntPtr format);
            [PreserveSig] int GetDevicePeriod(out long normal,out long minimum);
            [PreserveSig] int Start();
            [PreserveSig] int Stop();
            [PreserveSig] int Reset();
            [PreserveSig] int SetEventHandle(IntPtr handle);
            [PreserveSig] int GetService(ref Guid id,[MarshalAs(UnmanagedType.IUnknown)] out object value);
        }
        [ComImport,Guid("C8ADBD64-E71E-48A0-A4DE-185C395CD317"),InterfaceType(ComInterfaceType.InterfaceIsIUnknown)] private interface IAudioCaptureClient {
            [PreserveSig] int GetBuffer(out IntPtr data,out uint frames,out uint flags,out ulong devicePosition,out ulong clockPosition);
            [PreserveSig] int ReleaseBuffer(uint frames);
            [PreserveSig] int GetNextPacketSize(out uint frames);
        }
    }
}
