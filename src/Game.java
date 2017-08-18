import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import javax.swing.*;

public class Game extends JPanel implements Runnable, KeyListener, MouseListener, MouseMotionListener {
    static final int AIR=0, GRASS=1, DIRT=2, SAND=3, WATER=4, LOG=5, LEAF=6, STONE=7, IRON=8, DIAMOND=9, BEDROCK=10;
    static final int W=800, H=128, TILE=24, SEA=51;
    String[] names={"Air","Grass","Dirt","Sand","Water","Log","Leaf","Stone","Iron ore","Diamond ore","Bedrock","Planks","Sticks","Pickaxe"};
    Color[] colors={Color.WHITE,new Color(80,160,55),new Color(130,85,48),new Color(225,210,130),new Color(45,120,220),new Color(110,75,40),new Color(50,135,55),new Color(120,120,120),new Color(170,135,110),new Color(70,210,210),new Color(45,45,45),new Color(190,145,85)};
    int[][] world=new int[W][H];
    int[] inventory=new int[14];
    Random random=new Random();
    double px=50, py=70, vx, vy;
    boolean left,right,jump,onGround,craft;
    int selected=2, mouseX,mouseY, cameraX,cameraY;
    String message="Welcome!";
    long messageUntil;
    JFrame frame;

    public Game() {
        setPreferredSize(new Dimension(960,600));
        setFocusable(true);
        addKeyListener(this); addMouseListener(this); addMouseMotionListener(this);
        generate();
    }

    void generate() {
        px=50; py=70;
    }

    boolean solid(int b) { return b!=AIR && b!=WATER && b!=LEAF; }
    int block(int x,int y) { if(x<0 || x>=W || y<0) return BEDROCK; if(y>=H) return AIR; return world[x][y]; }
    boolean collides(double x,double y) {
        for(int bx=(int)Math.floor(x);bx<=(int)Math.floor(x+0.7);bx++)
            for(int by=(int)Math.floor(y);by<=(int)Math.floor(y+1.7);by++) if(solid(block(bx,by))) return true;
        return false;
    }
    void updateGame() {

    }
    public void run() {
        while(true) { updateGame(); repaint(); try { Thread.sleep(16); } catch(InterruptedException e) { return; } }
    }
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
    }
    void say(String s) { message=s; messageUntil=System.currentTimeMillis()+2500; }
    public void mousePressed(MouseEvent e) {

    }
    public void keyPressed(KeyEvent e) {

    }
    public void keyReleased(KeyEvent e) {

    }
    public void keyTyped(KeyEvent e) {} public void mouseReleased(MouseEvent e) {} public void mouseClicked(MouseEvent e) {} public void mouseEntered(MouseEvent e) {} public void mouseExited(MouseEvent e) {} public void mouseMoved(MouseEvent e) { mouseX=e.getX(); mouseY=e.getY(); } public void mouseDragged(MouseEvent e) {}
    public static void main(String[] args) {
        JFrame frame=new JFrame("2D Minecraft"); Game game=new Game(); frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); frame.add(game); frame.pack(); frame.setLocationRelativeTo(null); frame.setVisible(true); game.requestFocusInWindow(); new Thread(game).start();
    }
}
