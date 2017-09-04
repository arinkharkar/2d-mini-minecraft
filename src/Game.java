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
        double height=55;
        for(int x=0;x<W;x++) {
            height += (random.nextDouble()-0.5)*1.4;
            height=Math.max(49,Math.min(64,height));
            int biome=(x/100)%4;
            int top=(int)height;
            if(biome==0) top=48+(int)(3*Math.sin(x/15.0));
            if(biome==1) top=51+(int)(2*Math.sin(x/18.0));
            if(biome==2 || biome==3) top=57+(int)(3*Math.sin(x/21.0));
            for(int y=0;y<H;y++) {
                int b=AIR;
                if(y==0) b=BEDROCK;
                else if(y<=top) {
                    if(y==top) b=(biome==0 || biome==1 || top<=SEA+1)?SAND:GRASS;
                    else if(y>top-4) b=(biome==0 || biome==1)?SAND:DIRT;
                    else {
                        b=STONE;
                        if(y<42 && random.nextInt(75)==0) b=IRON;
                        if(y<23 && random.nextInt(180)==0) b=DIAMOND;
                    }
                } else if(y<=SEA) b=WATER;
                world[x][y]=b;
            }
        }
        for(int x=3;x<W-3;x++) {
            int biome=(x/100)%4;
            int top=H-7;
            while(top>0 && world[x][top]!=GRASS) top--;
            if(biome==3 && top>SEA+1 && random.nextInt(9)==0) {
                for(int y=top+1;y<=top+4;y++) world[x][y]=LOG;
                for(int dx=-2;dx<=2;dx++) for(int dy=3;dy<=6;dy++)
                    if(Math.abs(dx)+Math.abs(dy-4)<4 && x+dx>=0 && x+dx<W && top+dy<H && world[x+dx][top+dy]==AIR) world[x+dx][top+dy]=LEAF;
            }
        }
        int spawn=220;
        while(spawn<W-1 && world[spawn][SEA+1]==WATER) spawn++;
        int ground=H-2;
        while(ground>0 && !solid(world[spawn][ground])) ground--;
        px=spawn+0.1; py=ground+1;
    }

    boolean solid(int b) { return b!=AIR && b!=WATER && b!=LEAF; }
    int block(int x,int y) { if(x<0 || x>=W || y<0) return BEDROCK; if(y>=H) return AIR; return world[x][y]; }
    boolean collides(double x,double y) {
        for(int bx=(int)Math.floor(x);bx<=(int)Math.floor(x+0.7);bx++)
            for(int by=(int)Math.floor(y);by<=(int)Math.floor(y+1.7);by++) if(solid(block(bx,by))) return true;
        return false;
    }
    void updateGame() {
        vx=(right?0.12:0)-(left?0.12:0);
        if(!collides(px+vx,py)) px+=vx;
        if(jump && onGround) { vy=0.28; onGround=false; }
        vy=Math.max(-0.32,vy-0.015);
        double next=py+vy;
        if(!collides(px,next)) { py=next; onGround=false; }
        else { if(vy<0) onGround=true; vy=0; }
        cameraX=(int)(px*TILE-getWidth()/2);
        cameraY=(int)((H-py)*TILE-getHeight()/2);
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
