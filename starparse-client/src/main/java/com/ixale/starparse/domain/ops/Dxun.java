//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.ixale.starparse.domain.ops;

import com.ixale.starparse.domain.Combat;
import com.ixale.starparse.domain.Event;
import com.ixale.starparse.domain.Raid;
import com.ixale.starparse.domain.RaidBoss;
import com.ixale.starparse.domain.RaidBossName;
import com.ixale.starparse.domain.RaidBoss.BossUpgradeCallback;

public class Dxun extends Raid {
    private static final long RED_SM_8M = 4246176467517440L;
    private static final long RED_SM_16M = -100L;
    private static final long RED_HM_8M = 4330233272467456L;
    private static final long RED_HM_16M = -200L;
    private static final long BREACH_SM_8M = 4333218274738176L;
    private static final long BREACH_SM_16M = -1123L;
    private static final long BREACH_HM_8M = -1223L;
    private static final long BREACH_HM_16M = -1323L;
    private static final long TRANDOSHAN_SQUAD_SM_8M = 4245970309087232L;
    private static final long TRANDOSHAN_SQUAD_SM_16M = -1423L;
    private static final long TRANDOSHAN_SQUAD_HM_8M = -123L;
    private static final long TRANDOSHAN_SQUAD_HM_16M = -1623L;
    private static final long HUNTMASTER_SM_8M = 4265104388390912L;
    private static final long HUNTMASTER_SM_16M = -1523L;
    private static final long HUNTMASTER_HM_8M = 4330237567434752L;
    private static final long HUNTMASTER_HM_16M = -1723L;
    private static final long APEX_VG_SM_8M = 4282872668094464L;
    private static final long APEX_VG_SM_16M = -1233L;
    private static final long APEX_VG_HM_8M = 4350020186800128L;
    private static final long APEX_VG_HM_16M = -1263L;

    public Dxun() {
        super("The Nature of Progress");
        RaidBoss.add(this, RaidBossName.Red, new long[]{4246176467517440L}, new long[]{-100L}, new long[]{4330233272467456L}, new long[]{-200L}, (BossUpgradeCallback)null);
        RaidBoss.add(this, RaidBossName.BreachCI004, new long[]{4333218274738176L}, new long[]{-1123L}, new long[]{-1223L}, new long[]{-1323L}, (BossUpgradeCallback)null);
        RaidBoss.add(this, RaidBossName.TrandoshanSquad, new long[]{4245970309087232L, 4245978899021824L, 4245983193989120L, 4245987488956416L}, new long[]{-1423L}, new long[]{-123L}, new long[]{-1623L}, (BossUpgradeCallback)null);
        RaidBoss.add(this, RaidBossName.TheHuntmaster, new long[]{4265104388390912L, 4281661487316992L}, new long[]{-1523L}, new long[]{4330237567434752L}, new long[]{-1723L}, (BossUpgradeCallback)null);
        RaidBoss.add(this, RaidBossName.ApexVanguard, new long[]{4282872668094464L}, new long[]{-1233L}, new long[]{4350020186800128L}, new long[]{-1263L}, (BossUpgradeCallback)null);
    }

    public String getNewPhaseName(Event var1, Combat var2, String var3) {
        return var2.getBoss() == null ? null : null;
    }

    private String getNewPhaseNameForTyth(Event var1, Combat var2, String var3) {
        return null;
    }
}
