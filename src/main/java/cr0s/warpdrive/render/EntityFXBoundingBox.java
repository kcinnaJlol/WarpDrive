package cr0s.warpdrive.render;

import cr0s.warpdrive.data.Vector3;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import org.lwjgl.opengl.GL11;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class EntityFXBoundingBox extends AbstractEntityFX {
	
	private static final ResourceLocation TEXTURE = new ResourceLocation("warpdrive", "textures/particle/bounding_box.png");
	
	private Vector3 min;
	private Vector3 max;
	
	public EntityFXBoundingBox(final World world, final Vector3 position, final Vector3 min, final Vector3 max,
	                           final float red, final float green, final float blue, final int age) {
		super(world, position.x, position.y, position.z, 0.0D, 0.0D, 0.0D);
		this.setRBGColorF(red, green, blue);
		this.setSize(0.02F, 0.02F);
		this.noClip = true;
		this.motionX = 0.0D;
		this.motionY = 0.0D;
		this.motionZ = 0.0D;
		this.min = min;
		this.max = max;
		this.particleMaxAge = age;
		
		// kill the particle if it's too far away
		/*
		EntityLivingBase entityRender = Minecraft.getMinecraft().renderViewEntity;
		int visibleDistance = 300;

		if (!Minecraft.getMinecraft().gameSettings.fancyGraphics) {
			visibleDistance = 100;
		}

		if (entityRender.getDistance(posX, posY, posZ) > visibleDistance) {
			particleMaxAge = 0;
		}
		/**/
	}

	@Override
	public void onUpdate() {
		prevPosX = posX;
		prevPosY = posY;
		prevPosZ = posZ;
		
		if (particleAge++ >= particleMaxAge) {
			setDead();
		}
	}
	
	@Override
	public void renderParticle(final Tessellator tessellator, final float partialTick,
	                           final float cosYaw, final float cosPitch, final float sinYaw, final float sinSinPitch, final float cosSinPitch) {
		tessellator.draw();
		GL11.glPushMatrix();
		
		// final float rot = (worldObj.provider.getWorldTime() % (360 / rotationSpeed) + partialTick) * rotationSpeed;
		
        // alpha starts at 50%, vanishing to 10% during last ticks
		float alpha = 0.45F;
		if (particleMaxAge - particleAge <= 2) {
			alpha = 0.35F; // 0.45F - (1 - (particleMaxAge - particleAge)) * 0.35F;
		} else if (particleAge < 1) {
			alpha = 0.10F;
		}
		
		// final double relativeTime = worldObj.getTotalWorldTime() + partialTick;
		// final double uOffset = (float) (-relativeTime * 0.3D - MathHelper.floor_double(-relativeTime * 0.15D));
		// final double vOffset = (float) (-relativeTime * 0.2D - MathHelper.floor_double(-relativeTime * 0.1D));
		
		// box position
		final double relativeTime = Math.abs(worldObj.getTotalWorldTime() % 64L + partialTick) / 64.0D;
		final double sizeOffset = 0.01F * (1.0F + (float) Math.sin(relativeTime * Math.PI * 2));
		final double xMin = min.x - posX - sizeOffset;
		final double xMax = max.x - posX + sizeOffset;
		final double yMin = min.y - posY - sizeOffset;
		final double yMax = max.y - posY + sizeOffset;
		final double zMin = min.z - posZ - sizeOffset;
		final double zMax = max.z - posZ + sizeOffset;
		
		// texture coordinates
		final double uvScale = 1.0D;
		final double uv_xMin = xMin / uvScale + 0.5D;
		final double uv_xMax = xMax / uvScale + 0.5D;
		final double uv_yMin = yMin / uvScale + 0.5D;
		final double uv_yMax = yMax / uvScale + 0.5D;
		final double uv_zMin = zMin / uvScale + 0.5D;
		final double uv_zMax = zMax / uvScale + 0.5D;
		
		FMLClientHandler.instance().getClient().renderEngine.bindTexture(TEXTURE);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
		GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
		GL11.glDisable(GL11.GL_CULL_FACE);
		
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
		GL11.glDepthMask(false);
		
		final float xx = (float)(prevPosX + (posX - prevPosX) * partialTick - interpPosX);
		final float yy = (float)(prevPosY + (posY - prevPosY) * partialTick - interpPosY);
		final float zz = (float)(prevPosZ + (posZ - prevPosZ) * partialTick - interpPosZ);
		GL11.glTranslated(xx, yy, zz);
		final int brightness = 200;
		
		// x planes
		tessellator.startDrawingQuads();
		tessellator.setBrightness(brightness);
		tessellator.setColorRGBA_F(particleRed, particleGreen, particleBlue, alpha);
		tessellator.addVertexWithUV(xMin, yMin, zMin, uv_yMin, uv_zMin);
		tessellator.addVertexWithUV(xMin, yMin, zMax, uv_yMin, uv_zMax);
		tessellator.addVertexWithUV(xMin, yMax, zMax, uv_yMax, uv_zMax);
		tessellator.addVertexWithUV(xMin, yMax, zMin, uv_yMax, uv_zMin);
		tessellator.draw();
		
		tessellator.startDrawingQuads();
		tessellator.setBrightness(brightness);
		tessellator.setColorRGBA_F(particleRed, particleGreen, particleBlue, alpha);
		tessellator.addVertexWithUV(xMax, yMin, zMin, uv_yMin, uv_zMin);
		tessellator.addVertexWithUV(xMax, yMin, zMax, uv_yMin, uv_zMax);
		tessellator.addVertexWithUV(xMax, yMax, zMax, uv_yMax, uv_zMax);
		tessellator.addVertexWithUV(xMax, yMax, zMin, uv_yMax, uv_zMin);
		tessellator.draw();
		
		// y planes
		tessellator.startDrawingQuads();
		tessellator.setBrightness(brightness);
		tessellator.setColorRGBA_F(particleRed, particleGreen, particleBlue, alpha);
		tessellator.addVertexWithUV(xMin, yMin, zMin, uv_xMin, uv_zMin);
		tessellator.addVertexWithUV(xMin, yMin, zMax, uv_xMin, uv_zMax);
		tessellator.addVertexWithUV(xMax, yMin, zMax, uv_xMax, uv_zMax);
		tessellator.addVertexWithUV(xMax, yMin, zMin, uv_xMax, uv_zMin);
		tessellator.draw();
		
		tessellator.startDrawingQuads();
		tessellator.setBrightness(brightness);
		tessellator.setColorRGBA_F(particleRed, particleGreen, particleBlue, alpha);
		tessellator.addVertexWithUV(xMin, yMax, zMin, uv_xMin, uv_zMin);
		tessellator.addVertexWithUV(xMin, yMax, zMax, uv_xMin, uv_zMax);
		tessellator.addVertexWithUV(xMax, yMax, zMax, uv_xMax, uv_zMax);
		tessellator.addVertexWithUV(xMax, yMax, zMin, uv_xMax, uv_zMin);
		tessellator.draw();
		
		// z planes
		tessellator.startDrawingQuads();
		tessellator.setBrightness(brightness);
		tessellator.setColorRGBA_F(particleRed, particleGreen, particleBlue, alpha);
		tessellator.addVertexWithUV(xMin, yMin, zMin, uv_xMin, uv_yMin);
		tessellator.addVertexWithUV(xMin, yMax, zMin, uv_xMin, uv_yMax);
		tessellator.addVertexWithUV(xMax, yMax, zMin, uv_xMax, uv_yMax);
		tessellator.addVertexWithUV(xMax, yMin, zMin, uv_xMax, uv_yMin);
		tessellator.draw();
		
		tessellator.startDrawingQuads();
		tessellator.setBrightness(brightness);
		tessellator.setColorRGBA_F(particleRed, particleGreen, particleBlue, alpha);
		tessellator.addVertexWithUV(xMin, yMin, zMax, uv_xMin, uv_yMin);
		tessellator.addVertexWithUV(xMin, yMax, zMax, uv_xMin, uv_yMax);
		tessellator.addVertexWithUV(xMax, yMax, zMax, uv_xMax, uv_yMax);
		tessellator.addVertexWithUV(xMax, yMin, zMax, uv_xMax, uv_yMin);
		tessellator.draw();
		
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		GL11.glDepthMask(true);
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glPopMatrix();
		tessellator.startDrawingQuads();
		FMLClientHandler.instance().getClient().renderEngine.bindTexture(new ResourceLocation("textures/particle/particles.png"));
	}
}