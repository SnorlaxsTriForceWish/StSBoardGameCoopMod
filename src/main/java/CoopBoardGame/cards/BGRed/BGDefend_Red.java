package CoopBoardGame.cards.BGRed;

import CoopBoardGame.actions.player.GainBlockOnPlayerAction;
import CoopBoardGame.cards.AbstractBGCard;
import CoopBoardGame.characters.BGIronclad;
import CoopBoardGame.patches.CardTargetPatch;
import CoopBoardGame.targeting.PlayerTargetContext;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.common.GainBlockAction;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.AbstractCreature;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.localization.CardStrings;
import com.megacrit.cardcrawl.monsters.AbstractMonster;

public class BGDefend_Red extends AbstractBGCard {

    private static final CardStrings cardStrings = CardCrawlGame.languagePack.getCardStrings(
        "CoopBoardGame:BGDefend_R"
    );
    public static final String ID = "BGDefend_R";

    public BGDefend_Red() {
        super(
            "BGDefend_R",
            cardStrings.NAME,
            "red/skill/defend",
            1,
            cardStrings.DESCRIPTION,
            AbstractCard.CardType.SKILL,
            BGIronclad.Enums.BG_RED,
            AbstractCard.CardRarity.BASIC,
            AbstractCard.CardTarget.SELF
        );
        this.baseBlock = 1;
        this.tags.add(AbstractCard.CardTags.STARTER_DEFEND);
    }

    public void use(AbstractPlayer p, AbstractMonster m) {
        int blockAmount = Settings.isDebug ? 50 : this.block;

        if (this.upgraded) {
            // Upgraded: target any player via arrow targeting (CardTargetPatch.PLAYER)
            // The target is set in PlayerTargetContext by the targeting patches
            int targetPlayerId = PlayerTargetContext.requireCurrentTargetOrThrow();
            addToBot((AbstractGameAction) new GainBlockOnPlayerAction(targetPlayerId, blockAmount));
        } else {
            // Unupgraded: self-target only
            addToBot(
                (AbstractGameAction) new GainBlockAction(
                    (AbstractCreature) p,
                    (AbstractCreature) p,
                    blockAmount
                )
            );
        }
    }

    public void upgrade() {
        if (!this.upgraded) {
            upgradeName();
            upgradeBlock(1);
            this.rawDescription = cardStrings.UPGRADE_DESCRIPTION;
            initializeDescription();
            // Switch to player arrow targeting for upgraded version
            this.target = CardTargetPatch.PLAYER;
        }
    }

    public AbstractCard makeCopy() {
        return new BGDefend_Red();
    }
}
