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
    boolean mining;
    int mineX=-1, mineY=-1, mineTicks;
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
        if(mining && !craft) {
            int x=Math.floorDiv(mouseX+cameraX,TILE), y=H-1-Math.floorDiv(mouseY+cameraY,TILE);
            int b=block(x,y);
            if(x<0 || x>=W || y<0 || y>=H || Math.hypot(x+0.5-(px+0.35),y+0.5-(py+0.8))>5 || b==AIR || b==WATER || b==BEDROCK) {
                mineX=-1; mineY=-1; mineTicks=0;
            } else {
                if(x!=mineX || y!=mineY) { mineX=x; mineY=y; mineTicks=0; }
                int duration=miningTime(b);
                if(++mineTicks>=duration) {
                    if((b==IRON || b==DIAMOND) && (selected!=13 || inventory[13]==0)) say("Use a pickaxe for ore!");
                    else { world[x][y]=AIR; inventory[b]++; }
                    mineX=-1; mineY=-1; mineTicks=0;
                }
            }
        }
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
    int miningTime(int b) {
        boolean tool=selected==13 && inventory[13]>0;
        if(b==STONE || b==IRON || b==DIAMOND) return tool?25:150;
        if(b==LOG) return 45;
        if(b==LEAF) return 10;
        return 20;
    }
    public void run() {
        while(true) { updateGame(); repaint(); try { Thread.sleep(16); } catch(InterruptedException e) { return; } }
    }
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g=(Graphics2D)graphics;
        g.setColor(new Color(125,195,245)); g.fillRect(0,0,getWidth(),getHeight());
        g.setColor(new Color(255,250,170)); g.fillRect(45,35,54,54);
        g.setColor(new Color(255,225,65)); g.fillRect(51,41,42,42);
        g.setColor(new Color(255,240,100)); g.fillRect(57,46,25,25);
        g.setColor(new Color(230,245,255));
        g.fillRect(170,55,75,12); g.fillRect(185,47,38,12);
        g.fillRect(600,80,92,12); g.fillRect(620,72,45,12);
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
            if(mining && x==mineX && y==mineY && mineTicks>0) {
                int stage=mineTicks*4/miningTime(b);
                g.setColor(new Color(35,30,25,190));
                g.drawLine(sx+12,sy+1,sx+10,sy+9); g.drawLine(sx+10,sy+9,sx+15,sy+14);
                if(stage>=1) g.drawLine(sx+10,sy+9,sx+4,sy+13);
                if(stage>=2) { g.drawLine(sx+15,sy+14,sx+20,sy+20); g.drawLine(sx+15,sy+14,sx+10,sy+22); }
                if(stage>=3) { g.drawLine(sx+10,sy+9,sx+17,sy+5); g.drawLine(sx+4,sy+13,sx+1,sy+20); }
            }
        }
        int sx=(int)(px*TILE)-cameraX, sy=(int)((H-py-1.8)*TILE)-cameraY;
        g.setColor(new Color(230,180,140)); g.fillRect(sx+3,sy,12,13);
        g.setColor(new Color(40,140,180)); g.fillRect(sx+2,sy+13,14,16);
        g.setColor(new Color(50,55,110));
        int step=(left || right) && onGround ? (ticks/8)%2 : 0;
        g.fillRect(sx+2,sy+29,6,step==0?14:10); g.fillRect(sx+10,sy+29,6,step==0?10:14);
        g.setColor(Color.BLACK); g.fillRect(sx+11,sy+4,2,2);
        if(selected==13 && inventory[13]>0) {
            int swing=mining?(ticks/6)%2*4:0;
            g.setColor(new Color(115,70,35)); g.fillRect(sx+16,sy+18+swing,4,18);
            g.setColor(new Color(150,150,150)); g.fillRect(sx+12,sy+16+swing,13,5);
        }
        g.setColor(new Color(0,0,0,130)); g.fillRect(0,getHeight()-74,getWidth(),74);
        g.setFont(new Font("Arial",Font.PLAIN,14));
        int[] slots={1,2,3,4,5,6,7,8,9,11,12,13};
        String[] keys={"1","2","3","4","5","6","7","8","9","0","-","="};
        for(int i=0;i<slots.length;i++) {
            int item=slots[i], x=8+i*69;
            g.setColor(item==selected?Color.YELLOW:Color.LIGHT_GRAY); g.drawRect(x,getHeight()-65,65,54);
            if(item<colors.length) { g.setColor(colors[item]); g.fillRect(x+4,getHeight()-60,14,14); }
            else { g.setColor(item==12?new Color(165,110,55):Color.LIGHT_GRAY); g.fillRect(x+4,getHeight()-60,14,5); }
            g.setColor(Color.WHITE); g.setFont(new Font("Arial",Font.PLAIN,10));
            g.drawString(keys[i],x+23,getHeight()-49);
            String label=names[item];
            int space=label.indexOf(' ');
            if(space>=0) {
                g.drawString(label.substring(0,space),x+3,getHeight()-36);
                g.drawString(label.substring(space+1),x+3,getHeight()-26);
            } else g.drawString(label,x+3,getHeight()-31);
            g.drawString(""+inventory[item],x+4,getHeight()-14);
        }
        g.setFont(new Font("Arial",Font.PLAIN,14));
        g.setColor(Color.WHITE); g.drawString("Selected: "+names[selected]+"   A/D move, Space jump, hold left mouse to mine, right click to place, C craft",10,20);
        if(System.currentTimeMillis()<messageUntil) g.drawString(message,10,42);
        if(craft) {
            int ox=(getWidth()-530)/2, oy=90;
            g.setColor(new Color(45,40,35,240)); g.fillRect(ox,oy,530,310);
            g.setColor(Color.WHITE); g.drawRect(ox,oy,530,310);
            g.drawString("Crafting   -   click a recipe or press its key   -   C to close",ox+15,oy+25);
            String[] titles={"P: Planks x4", "S: Sticks x4", "K: Pickaxe x1"};
            String[] costs={"1 log (you have "+inventory[LOG]+")", "2 planks (you have "+inventory[11]+")", "3 stone + 2 sticks (you have "+inventory[STONE]+" and "+inventory[12]+")"};
            boolean[] ready={inventory[LOG]>=1,inventory[11]>=2,inventory[STONE]>=3 && inventory[12]>=2};
            for(int i=0;i<3;i++) {
                int y=oy+42+i*72;
                g.setColor(ready[i]?new Color(100,130,75):new Color(85,80,75)); g.fillRect(ox+15,y,500,62);
                g.setColor(Color.WHITE); g.drawRect(ox+15,y,500,62);
                g.drawString(titles[i]+(ready[i]?"   [Craft]":"   [Need materials]"),ox+28,y+23);
                g.drawString(costs[i],ox+28,y+45);
            }
            g.drawString("Planks: "+inventory[11]+"   Sticks: "+inventory[12]+"   Pickaxes: "+inventory[13]+"   (0 selects planks)",ox+15,oy+285);
        }
    }
    void makeRecipe(int recipe) {
        if(recipe==0 && inventory[LOG]>=1) { inventory[LOG]--; inventory[11]+=4; say("Made planks"); }
        else if(recipe==1 && inventory[11]>=2) { inventory[11]-=2; inventory[12]+=4; say("Made sticks"); }
        else if(recipe==2 && inventory[STONE]>=3 && inventory[12]>=2) { inventory[STONE]-=3; inventory[12]-=2; inventory[13]++; say("Made a pickaxe"); }
        else say("Not enough materials");
    }

    void say(String s) { message=s; messageUntil=System.currentTimeMillis()+2500; }
    public void mousePressed(MouseEvent e) {
        if(craft) {
            int ox=(getWidth()-530)/2, y=e.getY()-132;
            if(SwingUtilities.isLeftMouseButton(e) && e.getX()>=ox+15 && e.getX()<=ox+515 && y>=0 && y<206 && y%72<62) makeRecipe(y/72);
            return;
        }
        mouseX=e.getX(); mouseY=e.getY();
        if(SwingUtilities.isLeftMouseButton(e)) { mining=true; return; }
        int x=Math.floorDiv(e.getX()+cameraX,TILE), y=H-1-Math.floorDiv(e.getY()+cameraY,TILE);
        if(x<0 || x>=W || y<0 || y>=H || Math.hypot(x+0.5-(px+0.35),y+0.5-(py+0.8))>5) return;
        int b=world[x][y];
        if(SwingUtilities.isRightMouseButton(e) && selected<=11 && selected!=WATER) {
            if((b!=AIR && b!=WATER) || inventory[selected]<=0) return;
            world[x][y]=selected;
            if(collides(px,py)) { world[x][y]=b; return; }
            inventory[selected]--;
        }
    }

    public void keyPressed(KeyEvent e) {
        int k=e.getKeyCode();
        if(k==KeyEvent.VK_C) { craft=!craft; left=false; right=false; jump=false; mining=false; mineTicks=0; return; }
        if(craft) {
            if(k==KeyEvent.VK_P) makeRecipe(0);
            if(k==KeyEvent.VK_S) makeRecipe(1);
            if(k==KeyEvent.VK_K) makeRecipe(2);
            return;
        }
        if(k==KeyEvent.VK_A || k==KeyEvent.VK_LEFT) left=true;
        if(k==KeyEvent.VK_D || k==KeyEvent.VK_RIGHT) right=true;
        if(k==KeyEvent.VK_SPACE || k==KeyEvent.VK_W || k==KeyEvent.VK_UP) jump=true;
        if(k>=KeyEvent.VK_1 && k<=KeyEvent.VK_9) selected=k-KeyEvent.VK_0;
        if(k==KeyEvent.VK_0) selected=11;
        if(k==KeyEvent.VK_MINUS) selected=12;
        if(k==KeyEvent.VK_EQUALS) selected=13;
    }
    public void keyReleased(KeyEvent e) { int k=e.getKeyCode(); if(k==KeyEvent.VK_A || k==KeyEvent.VK_LEFT) left=false; if(k==KeyEvent.VK_D || k==KeyEvent.VK_RIGHT) right=false; if(k==KeyEvent.VK_SPACE || k==KeyEvent.VK_W || k==KeyEvent.VK_UP) jump=false; }
    public void keyTyped(KeyEvent e) {} public void mouseReleased(MouseEvent e) { if(SwingUtilities.isLeftMouseButton(e)) { mining=false; mineTicks=0; mineX=-1; mineY=-1; } } public void mouseClicked(MouseEvent e) {} public void mouseEntered(MouseEvent e) {} public void mouseExited(MouseEvent e) {} public void mouseMoved(MouseEvent e) { mouseX=e.getX(); mouseY=e.getY(); } public void mouseDragged(MouseEvent e) { mouseX=e.getX(); mouseY=e.getY(); }
    public static void main(String[] args) {
        JFrame frame=new JFrame("2D Minecraft"); Game game=new Game(); frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); frame.add(game); frame.pack(); frame.setLocationRelativeTo(null); frame.setVisible(true); game.requestFocusInWindow(); new Thread(game).start();
    }
}
