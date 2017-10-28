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
    int ticks;
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
        ticks++;
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
        Graphics2D g=(Graphics2D)graphics;
        g.setColor(new Color(125,195,245)); g.fillRect(0,0,getWidth(),getHeight());
        g.setColor(Color.YELLOW); g.fillOval(45,35,55,55);
        int start=Math.max(0,cameraX/TILE), end=Math.min(W,(cameraX+getWidth())/TILE+2);
        for(int x=start;x<end;x++) for(int y=0;y<H;y++) {
            int sy=(H-1-y)*TILE-cameraY;
            if(sy < -TILE || sy>getHeight()) continue;
            int b=world[x][y]; if(b==AIR) continue;
            int sx=x*TILE-cameraX;
            g.setColor(colors[b]); g.fillRect(sx,sy,TILE,TILE);
            if(b==GRASS) { g.setColor(colors[DIRT]); g.fillRect(sx,sy+7,TILE,TILE-7); }
            if(b==DIRT || b==GRASS || b==SAND || b==STONE || b==BEDROCK || b==LEAF) {
                g.setColor(new Color(0,0,0,30));
                for(int n=0;n<5;n++) {
                    int xx=Math.floorMod(x*31+y*17+n*13,20);
                    int yy=Math.floorMod(x*11+y*23+n*7,20);
                    if(b!=GRASS || yy>7) g.fillRect(sx+xx,sy+yy,3,3);
                }
            }
            if(b==LOG || b==11) {
                g.setColor(new Color(60,35,15,80));
                if(b==LOG) { g.fillRect(sx+5,sy,3,24); g.fillRect(sx+16,sy,3,24); }
                else { g.fillRect(sx,sy+7,24,2); g.fillRect(sx,sy+16,24,2); }
            }
            if(b==WATER) {
                g.setColor(new Color(210,235,255,90));
                int wave=(ticks/12+x*7+y*3)%16;
                g.fillRect(sx+wave,sy+5,7,2); g.fillRect(sx+(wave+9)%18,sy+16,5,2);
            }
            if(b==IRON || b==DIAMOND) { g.setColor(colors[STONE]); g.fillRect(sx,sy,TILE,TILE); g.setColor(colors[b]); g.fillRect(sx+4,sy+5,6,6); g.fillRect(sx+14,sy+13,6,6); }
            g.setColor(new Color(0,0,0,35)); g.drawRect(sx,sy,TILE,TILE);
        }
        int sx=(int)(px*TILE)-cameraX, sy=(int)((H-py-1.8)*TILE)-cameraY;
        g.setColor(new Color(230,180,140)); g.fillRect(sx+3,sy,12,13);
        g.setColor(new Color(40,140,180)); g.fillRect(sx+2,sy+13,14,16);
        g.setColor(new Color(50,55,110)); g.fillRect(sx+2,sy+29,14,14);
        g.setColor(Color.BLACK); g.fillRect(sx+11,sy+4,2,2);
        g.setColor(new Color(0,0,0,130)); g.fillRect(0,getHeight()-74,getWidth(),74);
        g.setFont(new Font("Arial",Font.PLAIN,14));
        for(int i=1;i<=9;i++) {
            int x=8+(i-1)*65; g.setColor(i==selected?Color.YELLOW:Color.LIGHT_GRAY); g.drawRect(x,getHeight()-65,60,54);
            g.setColor(colors[i]); g.fillRect(x+4,getHeight()-60,18,18); g.setColor(Color.WHITE);
            g.drawString(""+i+": "+inventory[i],x+4,getHeight()-26);
        }
        g.setColor(Color.WHITE); g.drawString("Selected: "+names[selected]+"   A/D move, Space jump, mouse mine/place, C craft",10,20);
        if(System.currentTimeMillis()<messageUntil) g.drawString(message,10,42);
        if(craft) {
            g.setColor(new Color(40,40,40,230)); g.fillRect(200,100,530,190);
            g.setColor(Color.WHITE); g.drawString("Crafting - press a key (C to close)",220,130);
            g.drawString("P: 1 log -> 4 planks   (place with 0)",220,160);
            g.drawString("S: 2 planks -> 4 sticks",220,190);
            g.drawString("K: 3 stone + 2 sticks -> pickaxe (needed for ores)",220,220);
            g.drawString("Planks: "+inventory[11]+"   Sticks: "+inventory[12]+"   Pickaxes: "+inventory[13],220,260);
        }
    }
    void say(String s) { message=s; messageUntil=System.currentTimeMillis()+2500; }
    public void mousePressed(MouseEvent e) {
        if(craft) return;
        int x=(e.getX()+cameraX)/TILE, y=H-1-(e.getY()+cameraY)/TILE;
        if(x<0 || x>=W || y<0 || y>=H || Math.hypot(x+0.5-(px+0.35),y+0.5-(py+0.8))>5) return;
        int b=world[x][y];
        if(SwingUtilities.isLeftMouseButton(e)) {
            if(b==AIR || b==WATER || b==BEDROCK) return;
            if((b==IRON || b==DIAMOND) && inventory[13]==0) { say("You need a pickaxe for ore!"); return; }
            world[x][y]=AIR; inventory[b]++;
        } else if(SwingUtilities.isRightMouseButton(e)) {
            if((b!=AIR && b!=WATER) || inventory[selected]<=0) return;
            int place=selected; world[x][y]=place;
            if(collides(px,py)) { world[x][y]=b; return; }
            inventory[selected]--;
        }
    }
    public void keyPressed(KeyEvent e) {
        int k=e.getKeyCode();
        if(k==KeyEvent.VK_C) { craft=!craft; return; }
        if(craft) {
            if(k==KeyEvent.VK_P && inventory[LOG]>=1) { inventory[LOG]--; inventory[11]+=4; say("Made planks"); }
            if(k==KeyEvent.VK_S && inventory[11]>=2) { inventory[11]-=2; inventory[12]+=4; say("Made sticks"); }
            if(k==KeyEvent.VK_K && inventory[STONE]>=3 && inventory[12]>=2) { inventory[STONE]-=3; inventory[12]-=2; inventory[13]++; say("Made a pickaxe"); }
            return;
        }
        if(k==KeyEvent.VK_A || k==KeyEvent.VK_LEFT) left=true;
        if(k==KeyEvent.VK_D || k==KeyEvent.VK_RIGHT) right=true;
        if(k==KeyEvent.VK_SPACE || k==KeyEvent.VK_W || k==KeyEvent.VK_UP) jump=true;
        if(k>=KeyEvent.VK_1 && k<=KeyEvent.VK_9) selected=k-KeyEvent.VK_0;
        if(k==KeyEvent.VK_0) selected=11;
    }
    public void keyReleased(KeyEvent e) { int k=e.getKeyCode(); if(k==KeyEvent.VK_A || k==KeyEvent.VK_LEFT) left=false; if(k==KeyEvent.VK_D || k==KeyEvent.VK_RIGHT) right=false; if(k==KeyEvent.VK_SPACE || k==KeyEvent.VK_W || k==KeyEvent.VK_UP) jump=false; }
    public void keyTyped(KeyEvent e) {} public void mouseReleased(MouseEvent e) {} public void mouseClicked(MouseEvent e) {} public void mouseEntered(MouseEvent e) {} public void mouseExited(MouseEvent e) {} public void mouseMoved(MouseEvent e) { mouseX=e.getX(); mouseY=e.getY(); } public void mouseDragged(MouseEvent e) {}
    public static void main(String[] args) {
        JFrame frame=new JFrame("2D Minecraft"); Game game=new Game(); frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); frame.add(game); frame.pack(); frame.setLocationRelativeTo(null); frame.setVisible(true); game.requestFocusInWindow(); new Thread(game).start();
    }
}
