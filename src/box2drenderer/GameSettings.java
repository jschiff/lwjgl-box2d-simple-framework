package box2drenderer;

import org.jbox2d.common.Vec2;

public class GameSettings {
	public final int screenWidth = 1024;
	public final int screenHeight = 768;

	public final float aspectRatio = (float)screenWidth / (float)screenHeight;
	
	public final float projectionHeight = 100;
	public final float projectionWidth = aspectRatio * projectionHeight;
	
	public final int drawRate = 60; // the number of frames to draw per second
	public final long drawDuration = 1000 / drawRate; // The amount of time a frame should take to draw
	
	public final int updateRate = 60; // The number of times per second to update the game world
	public final long updateDuration = 1000 / updateRate; // The amount of time a physics update should take
	
	// Physics simulation parameters
	protected final float timeStep = 1f / updateRate;
	protected final int velocityIterations = 6;
	protected final int positionIterations = 2;
	public final Vec2 gravity = new Vec2(0, -10);
	public final boolean allowSleep = true;
	public final boolean showHardwareCursor = false;
}
