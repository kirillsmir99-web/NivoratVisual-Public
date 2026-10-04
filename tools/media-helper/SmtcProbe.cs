using System;
using System.Threading;
using Windows.Media.Control;
using Windows.Foundation;
class SmtcProbe {
 public static void Main() {
   var op = GlobalSystemMediaTransportControlsSessionManager.RequestAsync();
   while(op.Status == AsyncStatus.Started) Thread.Sleep(20);
   var manager = op.GetResults();
   Console.WriteLine("sessions=" + manager.GetSessions().Count);
 }
}
