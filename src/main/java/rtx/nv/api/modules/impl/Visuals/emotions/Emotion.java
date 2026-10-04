package rtx.nv.api.modules.impl.Visuals.emotions;

public enum Emotion {
    WAVE("Помахать", 3.6f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            pose.rightArmX = -2.3f;
            pose.rightArmZ = 0.9f;
            pose.rightArmY = (float) Math.sin(progress * 12.0f) * 0.4f;
            pose.headZ = -0.1f;
            pose.headX = -0.1f;
        }
    },
    SALUTE("Салют", 3.0f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float hold = (float) Math.sin(progress * Math.PI);
            pose.rightArmX = -1.5f * hold;
            pose.rightArmZ = 1.15f * hold;
            pose.rightArmY = -0.75f * hold;
            pose.leftArmX = 0.0f;
            pose.leftArmZ = -0.06f;
            pose.leftArmY = 0.0f;
            pose.bodyX = -0.08f * hold;
            pose.headX = -0.1f * hold;
        }
    },
    SHY("Стеснение", 3.4f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            pose.headX = 0.3f;
            pose.headZ = 0.15f;
            pose.rightArmX = -0.7f;
            pose.rightArmY = -0.45f;
            pose.rightArmZ = 0.25f;
            pose.leftArmX = -0.7f;
            pose.leftArmY = 0.45f;
            pose.leftArmZ = -0.25f;
            pose.rightLegX = (float) Math.sin(progress * 8.0f) * 0.1f;
        }
    },
    DANCE("Танец", 4.2f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float cycle = progress * 14.0f;
            float beat = (float) Math.sin(cycle);
            float bounce = (float) Math.abs(Math.cos(cycle));
            pose.bodyY = beat * 0.25f;
            pose.bodyX = -bounce * 0.08f;
            pose.headY = -beat * 0.2f;
            pose.rightArmX = -1.0f + beat * 0.35f;
            pose.rightArmZ = 0.65f + bounce * 0.2f;
            pose.rightArmY = -0.2f;
            pose.leftArmX = -1.0f - beat * 0.35f;
            pose.leftArmZ = -0.65f - bounce * 0.2f;
            pose.leftArmY = 0.2f;
            pose.rightLegX = bounce * 0.15f;
            pose.leftLegX = -bounce * 0.15f;
        }
    },
    CLAP("Аплодисменты", 3.0f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float clap = (float) Math.abs(Math.sin(progress * 16.0f)) * 0.35f;
            pose.rightArmX = -1.25f;
            pose.rightArmZ = 0.5f;
            pose.rightArmY = -0.7f + clap;
            pose.leftArmX = -1.25f;
            pose.leftArmZ = -0.5f;
            pose.leftArmY = 0.7f - clap;
            pose.headX = -0.1f + (float) Math.sin(progress * 16.0f) * 0.05f;
        }
    },
    BOW("Поклон", 4.2f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float bow = (float) Math.sin(progress * Math.PI);
            pose.bodyX = bow * 0.65f;
            pose.headX = bow * 0.3f;
            pose.rightArmX = -0.8f * bow;
            pose.rightArmY = -0.6f * bow;
            pose.rightArmZ = 0.35f * bow;
            pose.leftArmX = 0.45f * bow;
            pose.leftArmY = -0.3f * bow;
            pose.leftArmZ = -0.2f * bow;
        }
    },
    FACEPALM("Фейспалм", 3.4f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float t = (float) Math.sin(progress * Math.PI);
            pose.headX = 0.35f * t;
            pose.headZ = -0.1f * t;
            pose.bodyX = 0.1f * t;
            pose.rightArmX = -1.75f * t;
            pose.rightArmY = -0.55f * t;
            pose.rightArmZ = 0.65f * t;
            pose.leftArmX = 0.15f * t;
            pose.leftArmZ = -0.1f * t;
        }
    },
    POINT("Указать", 2.6f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float hold = (float) Math.sin(progress * Math.PI);
            pose.rightArmX = -1.57f * hold;
            pose.rightArmY = 0.0f;
            pose.rightArmZ = 0.08f;
            pose.leftArmX = 0.1f * hold;
            pose.leftArmY = 0.35f * hold;
            pose.leftArmZ = -0.5f * hold;
            pose.headX = -0.05f * hold;
        }
    },
    CHEER("Радость", 3.2f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float jump = (float) Math.sin(progress * 12.0f);
            pose.bodyX = -0.08f;
            pose.headX = -0.25f + jump * 0.08f;
            pose.rightArmX = -2.6f + jump * 0.2f;
            pose.rightArmZ = 0.6f;
            pose.rightArmY = -0.2f;
            pose.leftArmX = -2.6f - jump * 0.2f;
            pose.leftArmZ = -0.6f;
            pose.leftArmY = 0.2f;
        }
    },
    LAUGH("Смех", 3.6f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float shake = (float) Math.sin(progress * 18.0f);
            pose.headX = -0.2f + shake * 0.12f;
            pose.bodyX = 0.22f + shake * 0.08f;
            pose.rightArmX = -0.85f;
            pose.rightArmY = -0.65f;
            pose.rightArmZ = 0.35f;
            pose.leftArmX = -0.85f;
            pose.leftArmY = 0.65f;
            pose.leftArmZ = -0.35f;
        }
    },
    CRY("Плач", 4.0f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float sob = (float) Math.sin(progress * 16.0f) * 0.06f;
            pose.headX = 0.4f + sob;
            pose.bodyX = 0.15f + sob * 0.5f;
            pose.rightArmX = -1.55f;
            pose.rightArmY = -0.45f;
            pose.rightArmZ = 0.45f;
            pose.leftArmX = -1.55f;
            pose.leftArmY = 0.45f;
            pose.leftArmZ = -0.45f;
        }
    },
    THINK("Раздумье", 3.8f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            pose.headX = 0.15f;
            pose.headZ = 0.18f;
            pose.rightArmX = -1.45f;
            pose.rightArmY = -0.45f;
            pose.rightArmZ = 0.55f;
            pose.leftArmX = -0.85f;
            pose.leftArmY = 0.75f;
            pose.leftArmZ = -0.3f;
        }
    },
    SHRUG("Недоумение", 3.2f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float shrug = (float) Math.sin(progress * Math.PI);
            pose.headZ = 0.18f * shrug;
            pose.headX = -0.05f * shrug;
            pose.bodyX = -0.05f * shrug;
            pose.rightArmX = -0.8f * shrug;
            pose.rightArmZ = 0.95f * shrug;
            pose.rightArmY = -0.3f * shrug;
            pose.leftArmX = -0.8f * shrug;
            pose.leftArmZ = -0.95f * shrug;
            pose.leftArmY = 0.3f * shrug;
        }
    },
    FLEX("Сила", 3.6f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float flex = (float) Math.sin(progress * Math.PI);
            pose.bodyX = -0.1f * flex;
            pose.headX = -0.15f * flex;
            pose.rightArmX = -1.57f * flex;
            pose.rightArmZ = 1.35f * flex;
            pose.rightArmY = -0.6f * flex;
            pose.leftArmX = -1.57f * flex;
            pose.leftArmZ = -1.35f * flex;
            pose.leftArmY = 0.6f * flex;
        }
    },
    HEART("Сердечко", 3.4f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float hold = (float) Math.sin(progress * Math.PI);
            pose.headX = 0.15f * hold;
            pose.rightArmX = -1.4f * hold;
            pose.rightArmY = -0.7f * hold;
            pose.rightArmZ = 0.5f * hold;
            pose.leftArmX = -1.4f * hold;
            pose.leftArmY = 0.7f * hold;
            pose.leftArmZ = -0.5f * hold;
        }
    },
    SURRENDER("Сдаюсь", 3.0f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float up = (float) Math.sin(progress * Math.PI);
            pose.headX = 0.15f * up;
            pose.rightArmX = -2.8f * up;
            pose.rightArmZ = 0.45f * up;
            pose.rightArmY = 0.0f;
            pose.leftArmX = -2.8f * up;
            pose.leftArmZ = -0.45f * up;
            pose.leftArmY = 0.0f;
        }
    },
    DISCO("Диско", 4.2f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float cycle = progress * 10.0f;
            float beat = (float) Math.sin(cycle);
            pose.bodyY = beat * 0.2f;
            pose.headY = beat * 0.15f;
            pose.rightArmX = -2.4f - beat * 0.3f;
            pose.rightArmZ = 0.7f + beat * 0.2f;
            pose.rightArmY = -0.3f;
            pose.leftArmX = 0.1f;
            pose.leftArmY = 0.4f;
            pose.leftArmZ = -0.5f;
        }
    },
    ZOMBIE("Зомби", 4.0f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float sway = (float) Math.sin(progress * 6.0f);
            pose.headX = 0.2f;
            pose.headY = sway * 0.15f;
            pose.bodyX = 0.1f;
            pose.rightArmX = -1.57f;
            pose.rightArmY = 0.0f;
            pose.rightArmZ = 0.08f;
            pose.leftArmX = -1.57f;
            pose.leftArmY = 0.0f;
            pose.leftArmZ = -0.08f;
        }
    },
    FIGHT("Боевая стойка", 3.5f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float guard = (float) Math.sin(progress * Math.PI);
            pose.bodyY = 0.3f * guard;
            pose.headY = -0.2f * guard;
            pose.rightArmX = -1.45f * guard;
            pose.rightArmY = -0.5f * guard;
            pose.rightArmZ = 0.35f * guard;
            pose.leftArmX = -1.35f * guard;
            pose.leftArmY = 0.4f * guard;
            pose.leftArmZ = -0.35f * guard;
        }
    },
    MEDITATE("Медитация", 4.5f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float breath = (float) Math.sin(progress * 6.0f) * 0.05f;
            pose.bodyX = -0.05f + breath;
            pose.headX = 0.15f;
            pose.rightArmX = -0.6f;
            pose.rightArmY = -0.4f;
            pose.rightArmZ = 0.6f;
            pose.leftArmX = -0.6f;
            pose.leftArmY = 0.4f;
            pose.leftArmZ = -0.6f;
        }
    },
    VICTORY("Победа", 3.5f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float pump = (float) Math.sin(progress * 14.0f) * 0.25f;
            pose.headX = -0.25f;
            pose.rightArmX = -2.5f + pump;
            pose.rightArmZ = 0.4f;
            pose.rightArmY = -0.2f;
            pose.leftArmX = 0.0f;
            pose.leftArmZ = -0.3f;
            pose.leftArmY = 0.2f;
        }
    },
    THREATEN("Угроза", 3.2f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float shake = (float) Math.sin(progress * 16.0f) * 0.2f;
            pose.headX = 0.15f;
            pose.rightArmX = -1.5f + shake;
            pose.rightArmY = -0.2f;
            pose.rightArmZ = 0.4f;
            pose.leftArmX = 0.1f;
            pose.leftArmY = 0.35f;
            pose.leftArmZ = -0.5f;
        }
    },
    SLEEPY("Зевота", 3.8f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float yawn = (float) Math.sin(progress * Math.PI);
            pose.headX = -0.3f * yawn;
            pose.rightArmX = -1.6f * yawn;
            pose.rightArmY = -0.6f * yawn;
            pose.rightArmZ = 0.5f * yawn;
            pose.leftArmX = -1.9f * yawn;
            pose.leftArmZ = -0.8f * yawn;
            pose.leftArmY = 0.2f * yawn;
        }
    },
    REST("Отдых", 4.5f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            pose.headX = -0.2f;
            pose.bodyX = -0.15f;
            pose.rightArmX = -1.8f;
            pose.rightArmY = -0.7f;
            pose.rightArmZ = 1.1f;
            pose.leftArmX = -1.8f;
            pose.leftArmY = 0.7f;
            pose.leftArmZ = -1.1f;
        }
    },
    DAB("Даб", 2.8f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float hold = (float) Math.sin(progress * Math.PI);
            pose.headX = 0.4f * hold;
            pose.headY = 0.5f * hold;
            pose.bodyY = 0.25f * hold;
            pose.rightArmX = 0.4f * hold;
            pose.rightArmY = -0.3f * hold;
            pose.rightArmZ = 1.6f * hold;
            pose.leftArmX = -1.6f * hold;
            pose.leftArmY = 0.7f * hold;
            pose.leftArmZ = -1.0f * hold;
        }
    },
    T_POSE("Т поза", 3.0f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float t = (float) Math.sin(progress * Math.PI);
            pose.rightArmX = 0.0f;
            pose.rightArmY = 0.0f;
            pose.rightArmZ = 1.57f * t;
            pose.leftArmX = 0.0f;
            pose.leftArmY = 0.0f;
            pose.leftArmZ = -1.57f * t;
            pose.headX = -0.05f * t;
        }
    },
    FLOSS("Флосс", 4.0f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float cycle = progress * 16.0f;
            float swing = (float) Math.sin(cycle);
            float hip = (float) Math.sin(cycle + Math.PI * 0.5) * 0.25f;
            pose.bodyY = hip;
            pose.rightArmX = -0.3f;
            pose.rightArmY = swing * 0.6f;
            pose.rightArmZ = 0.3f + swing * 0.4f;
            pose.leftArmX = -0.3f;
            pose.leftArmY = swing * 0.6f;
            pose.leftArmZ = -0.3f + swing * 0.4f;
            pose.rightLegX = hip * 0.3f;
            pose.leftLegX = -hip * 0.3f;
        }
    },
    SPIN("Вращение", 3.2f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float rot = (float) (progress * Math.PI * 4.0);
            pose.bodyY = (float) Math.sin(rot) * 0.6f;
            pose.headY = (float) Math.cos(rot) * 0.4f;
            pose.rightArmX = -0.4f;
            pose.rightArmZ = 0.8f;
            pose.rightArmY = 0.0f;
            pose.leftArmX = -0.4f;
            pose.leftArmZ = -0.8f;
            pose.leftArmY = 0.0f;
        }
    },
    SIT("Привал", 5.0f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float sit = (float) Math.sin(progress * Math.PI);
            pose.bodyX = 0.1f * sit;
            pose.rightLegX = -1.4f * sit;
            pose.leftLegX = -1.4f * sit;
            pose.rightLegZ = 0.25f * sit;
            pose.leftLegZ = -0.25f * sit;
            pose.rightArmX = -0.6f * sit;
            pose.rightArmY = -0.3f * sit;
            pose.rightArmZ = 0.3f * sit;
            pose.leftArmX = -0.6f * sit;
            pose.leftArmY = 0.3f * sit;
            pose.leftArmZ = -0.3f * sit;
        }
    },
    HERO("Герой", 3.8f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float hero = (float) Math.sin(progress * Math.PI);
            pose.headX = -0.2f * hero;
            pose.bodyX = -0.1f * hero;
            pose.rightArmX = 0.1f * hero;
            pose.rightArmY = -0.45f * hero;
            pose.rightArmZ = 0.75f * hero;
            pose.leftArmX = 0.1f * hero;
            pose.leftArmY = 0.45f * hero;
            pose.leftArmZ = -0.75f * hero;
            pose.rightLegZ = 0.15f * hero;
            pose.leftLegZ = -0.15f * hero;
        }
    },
    KICK("Удар с ноги", 3.0f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float kickPhase = (float) Math.sin(progress * Math.PI);
            float peak = (float) Math.pow(kickPhase, 2.5);
            pose.bodyX = -0.15f * peak;
            pose.bodyY = -0.25f * peak;
            pose.rightLegX = -1.55f * peak;
            pose.rightLegZ = 0.1f * peak;
            pose.leftLegX = 0.15f * peak;
            pose.leftArmX = -1.0f * peak;
            pose.leftArmY = 0.3f * peak;
            pose.leftArmZ = -0.5f * peak;
            pose.rightArmX = 0.3f * peak;
            pose.rightArmY = -0.2f * peak;
            pose.rightArmZ = 0.6f * peak;
        }
    },
    SHUFFLE("Шаффл", 4.0f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float step = (float) Math.cos(progress * 18.0f);
            float beat = (float) Math.sin(progress * 18.0f);
            pose.bodyY = beat * 0.15f;
            pose.rightLegX = step * 0.6f;
            pose.leftLegX = -step * 0.6f;
            pose.rightArmX = -step * 0.7f;
            pose.rightArmZ = 0.35f;
            pose.rightArmY = -0.1f;
            pose.leftArmX = step * 0.7f;
            pose.leftArmZ = -0.35f;
        }
    },
    GREETING("Приветствие", 3.8f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float raise = Math.min(1.0f, progress * 4.0f) * (1.0f - Math.max(0.0f, (progress - 0.75f) * 4.0f));
            pose.headX = 0.12f * (float) Math.sin(progress * Math.PI);
            pose.rightArmX = -1.85f * raise;
            pose.rightArmZ = 0.65f * raise;
            pose.rightArmY = (float) Math.sin(progress * 10.0f) * 0.35f * raise;
            pose.leftArmX = 0.1f * raise;
            pose.leftArmZ = -0.1f * raise;
        }
    },
    VICTORY_POSE("Победная поза", 4.0f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float t = (float) Math.sin(progress * Math.PI);
            pose.headX = -0.2f * t;
            pose.bodyX = -0.1f * t;
            pose.rightArmX = -2.7f * t;
            pose.rightArmZ = 0.75f * t;
            pose.rightArmY = -0.2f * t;
            pose.leftArmX = -2.7f * t;
            pose.leftArmZ = -0.75f * t;
            pose.leftArmY = 0.2f * t;
        }
    },
    CALM_DANCE("Спокойный танец", 4.8f) {
        @Override
        public void apply(EmotionPose pose, float progress) {
            float cycle = progress * 8.0f;
            float sway = (float) Math.sin(cycle);
            float sway2 = (float) Math.cos(cycle);
            pose.bodyY = sway * 0.18f;
            pose.bodyZ = sway * 0.08f;
            pose.headY = -sway * 0.15f;
            pose.headZ = sway * 0.06f;
            pose.rightArmX = -1.1f + sway2 * 0.25f;
            pose.rightArmZ = 0.7f + sway * 0.2f;
            pose.rightArmY = -0.3f + sway * 0.15f;
            pose.leftArmX = -1.1f - sway2 * 0.25f;
            pose.leftArmZ = -0.7f - sway * 0.2f;
            pose.leftArmY = 0.3f - sway * 0.15f;
            pose.rightLegX = sway * 0.1f;
            pose.leftLegX = -sway * 0.1f;
        }
    };

    private final String displayName;
    private final float duration;

    Emotion(String name, float dur) {
        this.displayName = name;
        this.duration = dur;
    }

    public String displayName() {
        return this.displayName;
    }

    public float duration() {
        return this.duration;
    }

    public static String[] displayNames() {
        Emotion[] values = values();
        String[] names = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].displayName;
        }
        return names;
    }

    public abstract void apply(EmotionPose pose, float progress);
}
