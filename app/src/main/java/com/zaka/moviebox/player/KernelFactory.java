package com.zaka.moviebox.player;

import android.content.Context;

/**
 * 内核工厂。
 * 阶段2：Exo + System；AUTO 默认 Exo，失败可切换 System。
 * IJK 需 native so，暂不默认开启。
 */
public final class KernelFactory {

    private KernelFactory() {}

    public static IKernel create(Context context, int kernelType) {
        IKernel kernel;
        switch (kernelType) {
            case IKernel.KERNEL_SYSTEM:
                kernel = new SystemKernel();
                break;
            case IKernel.KERNEL_IJK:
                // 未集成 ijk so 时回落 System，避免崩溃
                kernel = new SystemKernel();
                break;
            case IKernel.KERNEL_AUTO:
            case IKernel.KERNEL_EXO:
            default:
                kernel = new ExoKernel();
                break;
        }
        kernel.init(context);
        return kernel;
    }

    public static String displayName(int kernelType) {
        switch (kernelType) {
            case IKernel.KERNEL_AUTO: return "自动(Exo→系统)";
            case IKernel.KERNEL_EXO: return "ExoPlayer";
            case IKernel.KERNEL_IJK: return "IJK（需集成so）";
            case IKernel.KERNEL_SYSTEM: return "系统播放器";
            default: return "未知";
        }
    }

    public static final int[] OPTIONS = {
            IKernel.KERNEL_AUTO,
            IKernel.KERNEL_EXO,
            IKernel.KERNEL_SYSTEM,
    };

    public static String[] optionNames() {
        String[] names = new String[OPTIONS.length];
        for (int i = 0; i < OPTIONS.length; i++) {
            names[i] = displayName(OPTIONS[i]);
        }
        return names;
    }
}
