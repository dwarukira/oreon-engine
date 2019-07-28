package org.oreon.core.light;

import java.nio.FloatBuffer;

import org.oreon.core.context.BaseContext;
import org.oreon.core.math.Matrix4f;
import org.oreon.core.math.Vec3f;
import org.oreon.core.shadow.PssmCamera;
import org.oreon.core.util.BufferUtil;
import org.oreon.core.util.Constants;

public abstract class DirectionalLight extends Light{

	private Vec3f direction;
	private Vec3f ambient;
	private Matrix4f m_View;
	private Vec3f right;
	private Vec3f up;
	private PssmCamera[] splitLightCameras;
	
	private FloatBuffer floatBufferLight;
	private FloatBuffer floatBufferMatrices;
	private final int lightBufferSize = Float.BYTES * 12;

	private final int matricesBufferSize = Float.BYTES * 16 * 7 // 6 matrices, 16 floats per matrix 
										 + Float.BYTES * 24;	// 6 floats, 3 floats offset each
	
	protected DirectionalLight(){
		this(BaseContext.getConfig().getSunPosition().normalize(),
			new Vec3f(BaseContext.getConfig().getAmbient()),
			BaseContext.getConfig().getSunColor(),
			BaseContext.getConfig().getSunIntensity());
	}
	
	private DirectionalLight(Vec3f direction, Vec3f ambient, Vec3f color, float intensity) {
		
		super(color, intensity);
		this.direction = direction;
		this.setAmbient(ambient);
		
		up = new Vec3f(direction.getX(),0,direction.getZ());
		up.setY(-(up.getX() * direction.getX() + up.getZ() * direction.getZ())/direction.getY());
		
		if (direction.dot(up) != 0) 
//			log.warn("DirectionalLight vector up " + up + " and direction " +  direction + " not orthogonal");
			
		right = up.cross(getDirection()).normalize();
		m_View = new Matrix4f().View(getDirection(), up);	
		
		floatBufferMatrices = BufferUtil.createFloatBuffer(matricesBufferSize);
		
		splitLightCameras = new PssmCamera[Constants.PSSM_SPLITS];
		
		for (int i = 0; i<Constants.PSSM_SPLITS*2; i += 2){
			splitLightCameras[i/2] = new PssmCamera(Constants.PSSM_SPLIT_SHEME[i]*Constants.ZFAR,
											 Constants.PSSM_SPLIT_SHEME[i+1]*Constants.ZFAR);
			splitLightCameras[i/2].update(m_View, up, right);
			floatBufferMatrices.put(BufferUtil.createFlippedBuffer(splitLightCameras[i/2].getM_orthographicViewProjection()));
		}
		for (int i = 1; i<Constants.PSSM_SPLITS*2; i += 2){
			floatBufferMatrices.put(Constants.PSSM_SPLIT_SHEME[i]);
			floatBufferMatrices.put(0);
			floatBufferMatrices.put(0);
			floatBufferMatrices.put(0);
		}
	}
	
	public void update(){
		
		if (BaseContext.getCamera().isCameraRotated() || 
				BaseContext.getCamera().isCameraMoved()){
			updateShadowMatrices(false);
		}
	}
	
	public void updateShadowMatrices(boolean hasSunPositionChanged) {
		
		floatBufferMatrices.clear();
		
		for (int i=0; i<splitLightCameras.length; i++){
			
			if (i == splitLightCameras.length-1){
				if (hasSunPositionChanged){
					splitLightCameras[i].update(m_View, up, right);
				}
				floatBufferMatrices.put(BufferUtil.createFlippedBuffer(splitLightCameras[i].getM_orthographicViewProjection()));
			}
			else{
				splitLightCameras[i].update(m_View, up, right);
				floatBufferMatrices.put(BufferUtil.createFlippedBuffer(splitLightCameras[i].getM_orthographicViewProjection()));
			}
		}
	}
	
	public Vec3f getDirection() {
		return direction;
	}
	
	public void setDirection(Vec3f direction) {
		
		this.direction = direction;
		up = new Vec3f(direction.getX(),0,direction.getZ());
		up.setY(-(up.getX() * direction.getX() + up.getZ() * direction.getZ())/direction.getY());
		
		if (direction.dot(up) != 0) 
//			log.warn("DirectionalLight vector up " + up + " and direction " +  direction + " not orthogonal");
			
		right = up.cross(getDirection()).normalize();
		m_View = new Matrix4f().View(getDirection(), up);
		
		BaseContext.getConfig().setSunPosition(getDirection());
	}

	public Vec3f getUp() {
		return up;
	}

	public void setUp(Vec3f up) {
		this.up = up;
	}

	public Vec3f getRight() {
		return right;
	}

	public void setRight(Vec3f right) {
		this.right = right;
	}

	public Vec3f getAmbient() {
		return ambient;
	}

	public void setAmbient(Vec3f ambient) {
		this.ambient = ambient;
	}

	public Matrix4f getM_View() {
		return m_View;
	}

	public void setM_View(Matrix4f m_view) {
		this.m_View = m_view;
	}

	public PssmCamera[] getSplitLightCameras() {
		return splitLightCameras;
	}
	
	public int getLightBufferSize() {
		return lightBufferSize;
	}
	
	public int getMatricesBufferSize() {
		return matricesBufferSize;
	}

	public FloatBuffer getFloatBufferLight() {
		return floatBufferLight;
	}

	public void setFloatBufferLight(FloatBuffer floatBufferLight) {
		this.floatBufferLight = floatBufferLight;
	}

	public FloatBuffer getFloatBufferMatrices() {
		return floatBufferMatrices;
	}

	public void setFloatBufferMatrices(FloatBuffer floatBufferMatrices) {
		this.floatBufferMatrices = floatBufferMatrices;
	}
}
