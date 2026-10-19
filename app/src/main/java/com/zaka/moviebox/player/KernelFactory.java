package com.zaka.moviebox.player;

import android.content.Context;

/**
 * 内核工厂。
 * 阶段1：仅 Exo；AUTO 也落到 Exo。
 * 阶段2：按配置创建 IJK / System，并支持失败自动切换。
 */
public final class KernelFactory {

    private KernelFactory() {}

    public static IKernel create(Context context, int kernelType) {
        IKernel kernel;
        switch (kernelType) {
            case IKernel.KERNEL_IJK:
                // 阶段2 实现；目前回落到 Exo
                kernel = new ExoKernel();
                break;
            case IKernel.KERNEL_SYSTEM:
                // 阶段2 实现；目前回落到 Exo
                kernel = new ExoKernel();
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
            case IKernel.KERNEL_AUTO: return "自动";
            case IKernel.KERNEL_EXO: return "ExoPlayer";
            case IKernel.KERNEL_IJK: return "IJK（即将支持）";
            case IKernel.KERNEL_SYSTEM: return "系统（即将支持）";
            default: return "未知";
        }
    }

    public static final int[] OPTIONS = {
            IKernel.KERNEL_AUTO,
            IKernel.KERNEL_EXO,
            // IKernel.KERNEL_IJK,    // 阶段2 开放
            // IKernel.KERNEL_SYSTEM,
    };

    public static String[] optionNames() {
        return new String[]{
                displayName(IKernel.KERNEL_AUTO),
                displayName(IKernel.KERNEL_EXO),
        };
    }
}
