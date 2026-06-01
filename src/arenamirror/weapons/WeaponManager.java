package arenamirror.weapons;

import arenamirror.data.*;
import arenamirror.skills.*;

public class WeaponManager {
    public static WeaponManager instance;

    public WeaponManager() {
        instance = this;
    }

    public void equipWeapon(WeaponData weapon) {
        if (weapon == null || weapon.exclusiveSkills == null) return;
        for (SkillData s : weapon.exclusiveSkills) {
            SkillManager.instance.unlockSkill(s);
        }
    }
}
