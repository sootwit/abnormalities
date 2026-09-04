var Opcodes = Java.type('org.objectweb.asm.Opcodes');
var FieldInsnNode = Java.type('org.objectweb.asm.tree.FieldInsnNode');
var Double = Java.type('java.lang.Double');
var Float = Java.type('java.lang.Float');

var PI_D = [3.141592653589793, 1.5707963267948966, 0.7853981633974483, 6.283185307179586];
var PI_DN = ['PI', 'HALF_PI', 'QUARTER_PI', 'TAU'];
var PI_F = [3.1415927, 1.5707964, 0.7853982, 6.2831855];
var PI_FN = ['PI_F', 'HALF_PI_F', 'QUARTER_PI_F', 'TAU_F'];

function transformPi(classNode) {
    if (classNode.name.startsWith('com/abnormalities/pi/')) return classNode;
    var methods = classNode.methods;
    for (var m = 0; m < methods.size(); m++) {
        var method = methods.get(m);
        var insns = method.instructions;
        for (var i = 0; i < insns.size(); i++) {
            var insn = insns.get(i);
            var op = insn.getOpcode();
            if (op == Opcodes.LDC2_W && insn.cst != null && insn.cst instanceof Double) {
                var val = insn.cst.doubleValue();
                for (var d = 0; d < PI_D.length; d++) {
                    if (Double.compare(val, PI_D[d]) == 0) {
                        insns.set(insn, new FieldInsnNode(
                            Opcodes.GETSTATIC, 'com/abnormalities/pi/PiHooks',
                            PI_DN[d], 'D'
                        ));
                        break;
                    }
                }
            } else if (op == Opcodes.LDC && insn.cst != null && insn.cst instanceof Float) {
                var val = insn.cst.floatValue();
                for (var f = 0; f < PI_F.length; f++) {
                    if (Float.compare(val, PI_F[f]) == 0) {
                        insns.set(insn, new FieldInsnNode(
                            Opcodes.GETSTATIC, 'com/abnormalities/pi/PiHooks',
                            PI_FN[f], 'F'
                        ));
                        break;
                    }
                }
            }
        }
    }
    return classNode;
}

function initializeCoreMod() {
    var targets = [
        'net/minecraft/world/entity/Entity',
        'net/minecraft/world/phys/Vec3',
        'net/minecraft/world/phys/MathHelper',
        'net/minecraft/util/Mth',
        'net/minecraft/client/Camera',
        'net/minecraft/client/renderer/GameRenderer',
        'com/mojang/math/Quaternion',
        'net/minecraft/world/entity/LivingEntity',
        'net/minecraft/world/entity/player/Player',
        'net/minecraft/client/player/LocalPlayer',
        'net/minecraft/server/level/ServerPlayer',
        'net/minecraft/world/phys/AABB',
        'net/minecraft/core/Direction',
        'net/minecraft/world/phys/BlockHitResult',
        'net/minecraft/client/renderer/LevelRenderer',
        'com/mojang/blaze3d/vertex/PoseStack',
        'net/minecraft/world/entity/ai/navigation/GroundPathNavigation',
        'net/minecraft/world/level/lighting/DynamicGraphMinFixedPoint',
        'net/minecraft/world/level/chunk/LevelChunk',
        'net/minecraft/world/level/block/state/BlockState'
    ];
    var result = {};
    for (var t = 0; t < targets.length; t++) {
        result['pi_' + t] = {
            'target': { 'type': 'CLASS', 'name': targets[t] },
            'transformer': transformPi
        };
    }
    return result;
}
