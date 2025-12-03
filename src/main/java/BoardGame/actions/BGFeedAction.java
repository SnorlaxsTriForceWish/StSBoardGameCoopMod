package BoardGame.actions;

import BoardGame.monsters.bgbeyond.BGAwakenedOne;
import BoardGame.monsters.bgbeyond.BGDarkling;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.ApplyPowerAction;
import com.megacrit.cardcrawl.cards.DamageInfo;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.powers.StrengthPower;
import com.megacrit.cardcrawl.vfx.combat.FlashAtkImgEffect;

public class BGFeedAction extends AbstractGameAction {

    private final int strengthGain;
    private final DamageInfo damageInfo;

    public BGFeedAction(AbstractCreature target, DamageInfo damageInfo, int strengthGain) {
        this.damageInfo = damageInfo;
        this.strengthGain = strengthGain;
        setValues(target, damageInfo);
        this.actionType = ActionType.DAMAGE;
        this.duration = 0.1F;
    }

    public void update() {
        if (this.duration == 0.1F && this.target != null) {
            showAttackEffect();
            this.target.damage(this.damageInfo);

            if (isTargetKilled()) {
                grantStrengthToPlayer();
            }

            if (AbstractDungeon.getCurrRoom().monsters.areMonstersBasicallyDead()) {
                AbstractDungeon.actionManager.clearPostCombatActions();
            }

            this.isDone = true;
        }

        tickDuration();
    }

    private void showAttackEffect() {
        AbstractDungeon.effectList.add(
            new FlashAtkImgEffect(
                this.target.hb.cX,
                this.target.hb.cY,
                AttackEffect.NONE
            )
        );
    }

    private boolean isTargetKilled() {
        if (this.target.hasPower("Minion")) {
            return false;
        }

        AbstractMonster monster = (AbstractMonster) this.target;
        boolean isDead = monster.isDying || monster.currentHealth <= 0;

        // TODO: SlimeBoss should use halfdead state instead of disappearing until the next turn
        boolean canBypassHalfDead = this.target instanceof BGDarkling ||
                                     this.target instanceof BGAwakenedOne;
        boolean isActuallyDead = !this.target.halfDead || canBypassHalfDead;

        return isDead && isActuallyDead;
    }

    private void grantStrengthToPlayer() {
        // TODO degraffa: Use BG StrengthPower
        addToBot(
            new ApplyPowerAction(
                AbstractDungeon.player,
                AbstractDungeon.player,
                new StrengthPower(AbstractDungeon.player, this.strengthGain),
                this.strengthGain
            )
        );
    }
}
