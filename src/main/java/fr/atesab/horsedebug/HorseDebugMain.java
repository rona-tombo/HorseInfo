package fr.atesab.horsedebug;

import com.google.common.collect.Lists;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.util.math.RayTraceResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Locale;

//import net.minecraft.client.resources.I18n;

public class HorseDebugMain {
	private static final Logger log = LogManager.getLogger("HorseDebug");
	private static HorseDebugMain instance;

	private static void log(String message) {
		log.info("[" + log.getName() + "] " + message);
	}

	/**
	 * get this mod
	 */
	public static HorseDebugMain getMod() {
		return instance;
	}

	/**
	 * true if an API is already register for it
	 */
	public static boolean isAPIRegister() {
		return instance != null;
	}

	private static final double BAD_HP = 10; // min: 7.5

	private HorseDebugMain() {
		if (isAPIRegister())
			throw new IllegalStateException("An API is already register for this mod!");
	}

	private static final double BAD_JUMP = 2.75; // min: 1.2
	private static final double BAD_SPEED = 9; // min: ~7?
	private static final double EXCELLENT_HP = 14; // max: 15
	private static final double EXCELLENT_JUMP = 5; // max: 5.5?
	private static final double EXCELLENT_SPEED = 13;// max: 14.1?

	/**
	 * register an API for HorseDebug
	 *
	 * @throws IllegalStateException if an API is already register for it
	 */
	public static HorseDebugMain registerAPI(BuildAPI api) throws IllegalStateException{
		instance = new HorseDebugMain();
		log("Starting HorseDebug with " + api.getAPIName());
		return instance;
	}

	private void drawInventory(Minecraft mc, int posX, int posY, String[] addText, EntityLivingBase entity){
		int l = addText.length;
		if (l == 0)
			return;
		int sizeX = 0;
		int sizeY = 0;
		int itemSize = 20;
		if (mc.fontRenderer.FONT_HEIGHT * 2 + 2 > itemSize)
			// unused
			// itemSize = mc.fontRenderer.FONT_HEIGHT * 2 + 2;
			for(String s : addText){
			sizeY += mc.fontRenderer.FONT_HEIGHT + 1;
				int a = mc.fontRenderer.getStringWidth(s) + 10;
				if(a > sizeX)
				sizeX = a;
		}
		if (entity != null) {
			sizeX += 100;
			if (sizeY < 100)
				sizeY = 100;
		}
		MainWindow mw = mc.mainWindow;
		posX += 5;
		posY += 5;
		if (posX + sizeX > mw.getScaledWidth())
			posX -= sizeX + 10;
		if (posY + sizeY > mw.getScaledHeight())
			posY -= sizeY + 10;
		int posY1 = posY + 5;
		for(String s : addText){
			mc.fontRenderer.drawStringWithShadow(s, posX + 5, posY1, 0xffffffff);
			posY1 += (mc.fontRenderer.FONT_HEIGHT + 1);
		}
		if (entity != null){
			GlStateManager.color3f(1.0F, 1.0F, 1.0F);
			GuiInventory.drawEntityOnScreen(posX + sizeX - 55, posY + 105, 50, 50, 0, entity);
		}
	}

	private String[] getEntityData(EntityLivingBase entity){
		List<String> text = Lists.newArrayList();
		text.add("\u00a7b" + entity.getDisplayName().getFormattedText());
		if (entity instanceof AbstractHorse) {
			AbstractHorse baby = (AbstractHorse) entity;

			double yVelocity = baby.getHorseJumpStrength();
			double jumpHeight = 0;
			while (yVelocity > 0) {
				jumpHeight += yVelocity;
				yVelocity -= 0.08;
				yVelocity *= 0.98;
			}
			text.add(
					// I18nがうまく行ってなかったので除けてハードコードする
					// I18n.format("gui.act.invView.horse.jump") +
					"ジャンプ : "
							+ getFormattedText(jumpHeight, BAD_JUMP, EXCELLENT_JUMP) + " " + "("
							+ significantNumbers(baby.getHorseJumpStrength()) + " iu)");
			text.add(// I18n.format("gui.act.invView.horse.speed") +
					"スピード : "
							+ getFormattedText(
							//baby.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue() * 43,
							baby.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getValue() * 43,
							BAD_SPEED, EXCELLENT_SPEED)
							+ " m/s " + "("
					+ significantNumbers(
							baby.getAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getValue())
							+ " iu)");
			text.add(//I18n.format("gui.act.invView.horse.health") +
					"体力 : "
							+ getFormattedText((baby.getMaxHealth() / 2D), BAD_HP, EXCELLENT_HP) + " HP");
		}
		return text.toArray(new String[0]);
	}

	private static String getFormattedText(double value, double bad, double exellent) {
		return new String(new char[] { '\u00a7', ((value > exellent) ? '6' : (value < bad) ? 'c' : 'a') })
				+ significantNumbers(value);
	}

	private static String significantNumbers(double d) {
		boolean a;
		if (a = d < 0) {
			d *= -1;
		}
		int d1 = (int) (d);
		d %= 1;
		String s = String.format("%.3G", d);
		if (s.length() > 0)
			s = s.substring(1);
		if (s.contains("E+"))
			s = String.format(Locale.US, "%.0f", Double.valueOf(String.format("%.3G", d)));
		return (a ? "-" : "") + d1 + s;
	}

	public void renderOverlay(){
		Minecraft mc = Minecraft.getInstance();
		if (!mc.gameSettings.showDebugInfo)
			return;
		MainWindow mw = mc.mainWindow;
		if (mc.player.getRidingEntity() instanceof AbstractHorse) {
			AbstractHorse baby = (AbstractHorse) mc.player.getRidingEntity();
			drawInventory(mc, mw.getScaledWidth(), mw.getScaledHeight(), getEntityData(baby), baby);
		} else {
			RayTraceResult obj = mc.objectMouseOver;
			if(obj != null && obj.type.equals(RayTraceResult.Type.ENTITY)
					&& obj.entity instanceof EntityLivingBase){
				drawInventory(mc, mw.getScaledWidth(), mw.getScaledHeight(),
						getEntityData((EntityLivingBase) obj.entity), (EntityLivingBase) obj.entity);
			}
		}
	}

}
