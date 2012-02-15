/**
 * 
 */
package box2drenderer;

import java.awt.Event;
import java.util.ArrayList;

import javax.swing.text.StyledEditorKit.BoldAction;

import org.jbox2d.common.MathUtils;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;
import org.lwjgl.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.*;
import org.lwjgl.util.Color;
import org.lwjgl.util.ReadableColor;

// This line lets me say stuff like glOrtho() instead of GL11.glOrtho()
import static org.lwjgl.opengl.GL11.*;

/**
 * @author Jeremy Schiff
 *
 */
public class Game {
	
	protected final GameSettings settings;
	
	protected final long gameStartTime = getTime();
	
	private ArrayList<Boolean> mouseStates;
	private ArrayList<Boolean> keyStates; 
	
	protected World physicsWorld;
	private float _gameSpeed = 1.0f;
	private boolean _quit = false;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Game game = new Game(new GameSettings());
		game.run();
	}
	
	public Game(GameSettings settings){
		// Set up the world
		this.settings = settings;
		physicsWorld = new World(settings.gravity, settings.allowSleep);
	}
	
	private void setupProjection(float projectionWidth, float projectionHeight) {
		glMatrixMode(GL_PROJECTION);
		glLoadIdentity();
		glOrtho(0, projectionWidth, 0, projectionHeight, 1, -1);
		glMatrixMode(GL_MODELVIEW);
		
		glLoadIdentity();
	}
	
	public void run(){
		try {
			DisplayMode dm = new DisplayMode(settings.screenWidth, settings.screenHeight);
			Display.setDisplayMode(dm);
			Display.create();
			setupProjection(settings.projectionWidth, settings.projectionHeight);
		}
		catch (LWJGLException e) {
			System.err.println(e.getMessage());
		}
		Mouse.setGrabbed(!settings.showHardwareCursor);
		Mouse.setCursorPosition(settings.screenWidth / 2, settings.screenHeight / 2);
		
		// Initialize mouse state
		Mouse.poll();
		int mouseButtonCount = Mouse.getButtonCount();
		mouseStates = new ArrayList<Boolean>(mouseButtonCount);
		for (int i = 0; i < mouseButtonCount; i++){
			mouseStates.add(i, Mouse.isButtonDown(i));
		}
		
		Keyboard.poll();
		// Initialize keyboard state
		Keyboard.enableRepeatEvents(false);
		int keyCount = Keyboard.getKeyCount();
		keyStates = new ArrayList<Boolean>(keyCount);
		for (int i = 0; i < keyCount; i++){
			keyStates.add(i, Keyboard.isKeyDown(i));
		}
		
		long start, end, frameDuration;
		// OH HEY IT'S THE MAIN GAME LOOP
		while (!Display.isCloseRequested() && !_quit) {
			// Handle input events
			start = getGameTime();
			
			handleInput();
			update();
			draw();
			
			Display.update();
			end = getGameTime();
			frameDuration = end - start;
			
			// Sleep for any extra time.
			if (frameDuration < settings.drawDuration){
				try {
					Thread.sleep(settings.drawDuration - frameDuration);
				} catch (Exception e) {
					System.err.println(e.getMessage());
				}
			}
			else{
				System.err.println("Warning, the game is not being drawn or updated fast enough to achieve the desired refresh rate.");
			}
		}
		
		Display.destroy();
	}
	
	protected void draw(){
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
		drawPhysicsWorld();
		DrawingTool.drawCircle(screenCoordsToProjectionCoords(Mouse.getX(), Mouse.getY()), 5, 5, 50, new DrawingArgs(Color.CYAN, true));
	}
	
	protected void update(){
		if (_gameSpeed > 0){
			physicsWorld.step(settings.timeStep * _gameSpeed, settings.velocityIterations, settings.positionIterations);	
		}
	}
	
	// Gets the current system time in milliseconds
	private static long getTime(){
		return System.nanoTime() / 1000000;
	}
	
	// Returns the amount of time that has passed since the game started
	public long getGameTime(){
		return getTime() - gameStartTime;
	}
	
	protected void handleInput() {
		// Handle keyboard input
		while (Keyboard.next()){
			int key = Keyboard.getEventKey();
			boolean lastStatus = keyStates.get(key);
			boolean down = Keyboard.getEventKeyState();
			if (lastStatus != down){
				keyStates.set(key, down);
				if (down){
					onKeyDown(key);
				}
				else {
					onKeyUp(key);
				}
			}
		}
		
		// Handle mouse input
		while (Mouse.next()){
			int button = Mouse.getEventButton();
			
			// Mouse click event
			if (button != -1){
				boolean down = Mouse.getEventButtonState();
				boolean lastStatus = mouseStates.get(button);
				if (lastStatus != down){
					mouseStates.set(button, down);
					if (down){
						onMouseClick(button);
					}
					else {
						onMouseRelease(button);
					}
				}
			}
			
			// Mouse move event
			int dy = Mouse.getDY();
			int dx = Mouse.getDX();
			if (dy > 0 || dx > 0){
				int x = Mouse.getEventX();
				int y = Mouse.getEventY();
				Vec2 mousePos = screenCoordsToProjectionCoords(x, y);
				onMouseMoved(dx, dy, mousePos);
			}
			
			// Mouse scroll event
			if(Mouse.hasWheel()){
				int scroll = Mouse.getEventDWheel();
				if (scroll > 0){
					onMouseScroll(scroll);
				}
			}
		}
	}

	// Draw the world
	protected void drawPhysicsWorld(){
		// Iterate over all the bodies in the world and draw each of their fixtures
		for (Body body = physicsWorld.getBodyList(); body != null; body = body.getNext()){
			DrawingTool.drawBody(body);
		}
	}
	
	// subclass should override these methods to deal with input
	protected void onMouseScroll(int scroll) {
	}

	protected void onMouseClick(int button){
	}
	
	protected void onMouseRelease(int button){
		
	}
	
	protected void onKeyDown(int key){
		switch (key) {
		case Keyboard.KEY_ESCAPE:
			endGame();
			break;
		default:
			break;
		}
	}
	
	protected void onKeyUp(int key){
		
	}
	
	protected Vec2 screenCoordsToProjectionCoords(int x, int y){
		float conversionRatio = settings.projectionHeight / settings.screenHeight;
		
		float posX = x * conversionRatio;
		float posY = y * conversionRatio;
		
		return new Vec2(posX, posY);
	}
	
	protected int[] projectionCoordsToScreenCoords(Vec2 pos){
		int[] ret = new int[2];
		
		float conversionRatio = settings.screenWidth / settings.projectionWidth;
		
		
		ret[0] = MathUtils.round(pos.x * conversionRatio);
		ret[1] = MathUtils.round(pos.y * conversionRatio);
				
		return ret;
	}
	
	protected void onMouseMoved(int dx, int dy, Vec2 endPosition){
	}
	
	public boolean getMouseButtonState(int button){
		return mouseStates.get(button);
	}
	
	public boolean getKeyState(int key){
		return keyStates.get(key);
	}
	
	protected void setGameSpeed(float factor){
		_gameSpeed = factor;
	}
	
	protected void pausePhysics(){
		setGameSpeed(0);
	}
	
	protected float getGameSpeed(){
		return _gameSpeed;
	}
	
	public void endGame(){
		_quit = true;
	}
}
