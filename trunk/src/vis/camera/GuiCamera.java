package vis.camera;

import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.vecmath.Point2d;

import org.apache.log4j.Logger;

import ui.gui.Gui;
import vis.WrongMatrixStackException;
import vis.input.Action;
import vis.input.ActionHandler;
import vis.input.NotResponsibleException;
import vis.model.IEvent;
import vis.model.VidisMouseEvent;

public class GuiCamera implements ICamera, ActionHandler {
	
	private static Logger logger = Logger.getLogger( GuiCamera.class );	
	
	/* render target */
	private Rectangle target;
	private double left;
	private double right;
	private double top;
	private double bottom;
	private Gui gui;
	/* Position of the camera */
	private double posx;
	private double posy;
	private double step = 0.1; // Scrollspeed
	/* Zoom */
	private double zoom;
	/**
	 * used for calc3DMousePoint
	 */
	private DoubleBuffer model = DoubleBuffer.allocate(16), proj = DoubleBuffer
			.allocate(16);
	private IntBuffer view = IntBuffer.allocate(4);
	
	public GuiCamera( Gui gui ){
		this.gui = gui;
		this.posx = 0;
		this.posy = 0;
		this.zoom = 1.0;
	}
	public void init(GL gl) {
		gl.glClear(GL.GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL.GL_BLEND);
		gl.glViewport((int)target.getX(), (int)target.getY(), (int)target.getWidth(), (int)target.getHeight());
	}
	public void applyProjectionMatrix(GL gl) throws WrongMatrixStackException {
		GLU glu = new GLU();
	
		glu.gluOrtho2D( left, right, bottom, top );
//		System.out.println("aspect="+aspect+", gluOrtho2d( "+left+", "+right+", "+bottom+", "+top+" )");
	}
	public void applyViewMatrix(GL gl) {
//		gl.glTranslated(posx, -posy +zoom/2, zoom);
		
		// matrizen auslesen f�r click berechnung
		gl.glGetDoublev(GL.GL_MODELVIEW_MATRIX, model);
		gl.glGetDoublev(GL.GL_PROJECTION_MATRIX, proj);
		gl.glGetIntegerv(GL.GL_VIEWPORT, view);
	}
	public String toString() {
		return "GuiCamera at ("+posx+", "+posy+") Zoom: "+zoom;
	}
	public void setPosition(double posx, double posy){
		this.posx = posx;
		this.posy = posy;
	}
	public double getZoom() {
		return zoom;
	}
	public void setZoom(double zoom) {
		this.zoom = zoom;
	}
	public double getPosx() {
		return posx;
	}
	public void setPosx(double posx) {
		this.posx = posx;
	}
	public double getPosy() {
		return posy;
	}
	public void setPosy(double posy) {
		this.posy = posy;
	}
	public void handleAction(Action a) throws NotResponsibleException {
		switch (a){
		case SCROLL_UP:
			this.posy+=step; 
			break;
		case SCROLL_DOWN:
			this.posy-=step;
			break;
		case SCROLL_LEFT:
			this.posx-=step;
			break;
		case SCROLL_RIGHT:
			this.posx+=step;
			break;
		case ZOOM_IN:
			this.zoom-=step;
			break;
		case ZOOM_OUT:
			this.zoom+=step;
			break;
		default:
			throw new NotResponsibleException();
		}
		
	}
	public Rectangle getTarget() {
		return target;
	}
	public void setTarget(Rectangle target) {
		this.target = target;
	}
	public void reshape(int x, int y, int width, int height) {
		

		this.target = new Rectangle(x, y, width, height);
		// FIXME works for now
		double aspect = target.getWidth() / (double) target.getHeight();
		double dwidth = width / 10d;
		double dheight = height / 10d;
		left = -dwidth / 2d;
		right = dwidth / 2d;
		top = -dheight / 2d;
		bottom = dheight /2d;
		
//		System.out.println("bounds: x="+left+", y="+top+", height="+dheight+", width="+dwidth);
		gui.updateBounds(left, top, dheight, dwidth);
		gui.fireEvent( new IEvent(){
			public int getID() {
				return IEvent.ResizeEvent;
			}
		});
		
	}
	public void handleMouseEvent( MouseEvent e ) {
		logger.debug("handleMouseEvent() id="+e.getID());
		VidisMouseEvent m = new VidisMouseEvent();
		m.where = convert2Dto3D(e.getX(), e.getY());
		gui.fireEvent( m );
	}
	
	private Point2d convert2Dto3D( int x, int y ) {
		double xrel = (double) x / (double) target.getWidth();
		double yrel = (double) y / (double) target.getHeight();
		
		double xout = xrel * target.getWidth() / 2d;
		double yout = yrel * target.getHeight() / 2d;
		
		return new Point2d( x / 10d, y / 10d);
	}
	
/*
	public Vector4d calc3DMousePoint(Point p) {
		DoubleBuffer point1 = DoubleBuffer.allocate(3), point2 = DoubleBuffer
				.allocate(3);
		boolean p1, p2;
		GLU glu = new GLU();
		p1 = glu.gluUnProject(p.getX(), view.get(3) - p.getY() - 1, 0.0, model,
				proj, view, point1);
		p2 = glu.gluUnProject(p.getX(), view.get(3) - p.getY() - 1, 1.0, model,
				proj, view, point2);
		if (!p1 & !p2)
			return null;
		Vector4d P1 = Vector4d.fromDoubleBuffer(point1, 1);
		Vector4d P2 = Vector4d.fromDoubleBuffer(point2, 1);
		// Click Ray
		Vector4d P1P2 = VMath.minus(P2, P1);

		Vector4d E0 = new Vector4d(0, 0, 0, 1);
		Vector4d EX = new Vector4d(1, 0, 0, 1);
		Vector4d EY = new Vector4d(0, 1, 0, 1);
		// Ebene
		Vector4d E0EX = VMath.minus(EX, E0);
		Vector4d E0EY = VMath.minus(EY, E0);

		Vector4d EBENE = VMath.kreuz(E0EX, E0EY);
		EBENE = new Vector4d(0, 0, 1, 0);
		// Schnittpunkt
		// 
		// Gerade:
		// P1 + v * P1P2
		// x = P1.x + v * P1P2.x
		// ...

		// Ebene:
		// EBENE.x * x + EBENE.y * y + EBENE.z * z = 0

		// EBENE.x * (P1.x + v * P1P2.x) +
		// EBENE.y * (P1.y + v * P1P2.y) +
		// EBENE.z * (P1.z + v * P1P2.z) = 0

		// EBENE.x * P1.x + EBENE.y * P1.y + EBENE.z * P1.z
		// --------------------------------------------------------- = y
		// - EBENE.x * P1P2.x - EBENE.y * P1P2.y - EBENE.z * P1P2.z

		double y = (EBENE.x * P1.x + EBENE.y * P1.y + EBENE.z * P1.z)
				/ (-EBENE.x * P1P2.x - EBENE.y * P1P2.y - EBENE.z * P1P2.z);

		return VMath.add(P1, VMath.mul(y, P1P2));
	}
	*/
}
