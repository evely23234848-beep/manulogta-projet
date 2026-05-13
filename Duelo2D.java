package duelo2d;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.ArrayList;
import java.util.Random;

public class Duelo2D extends JPanel implements ActionListener, KeyListener {
    private enum Estado { MENU, JUEGO }
    private Estado estadoActual = Estado.MENU;
    private int seleccion1 = 0, seleccion2 = 0;
    private String[] armas = {"EQUILIBRADA", "PESADA", "RÁPIDA"};
    private boolean ia1 = false, ia2 = true;

    // 🔥 SKINS
    private Image skin1, skin2;

    private int x1 = 50, y1 = 250, vida1 = 200, armadura1 = 80, dmg1 = 12, velMov1 = 5;
    private int x2 = 1100, y2 = 250, vida2 = 200, armadura2 = 80, dmg2 = 12, velMov2 = 5;
    private long lastShot1 = 0, lastShot2 = 0, cooldown1 = 400, cooldown2 = 400;
    
    private Rectangle powerUp;
    private String tipoPowerUp = "";
    private ArrayList<Rectangle> coberturas = new ArrayList<>();
    private int powerUpTimer = 0;
    
    private int timerEscudo1, timerRafaga1, timerVampiro1;
    private int timerEscudo2, timerRafaga2, timerVampiro2;
    private int regenTimer1 = 0, regenTimer2 = 0;
    private final int ESPERA_REGEN = 120, MAX_ARMADURA = 80;
    private double rotacion1 = 0, rotacion2 = 0;
    private boolean up1, down1, left1, right1, up2, down2, left2, right2;
    
    private ArrayList<Rectangle> balas1 = new ArrayList<>();
    private ArrayList<Rectangle> balas2 = new ArrayList<>();
    private Random rnd = new Random();
    private Timer timer;

    public Duelo2D() {
        this.setFocusable(true);
        this.addKeyListener(this);

        // Intento de carga inicial (opcional)
        try {
            skin1 = new ImageIcon(getClass().getResource("/skins/j1.png")).getImage();
            skin2 = new ImageIcon(getClass().getResource("/skins/j2.png")).getImage();
        } catch (Exception e) {
            System.out.println("Skins iniciales no encontradas. Usa C y N en el menú.");
        }

        coberturas.add(new Rectangle(580, 150, 40, 150));
        coberturas.add(new Rectangle(580, 450, 40, 150));
        timer = new Timer(15, this);
        timer.start();
    }

    // 🔥 MÉTODO PARA SELECCIONAR ARCHIVO
    private void seleccionarSkin(int jugador) {
        JFileChooser buscador = new JFileChooser();
        FileNameExtensionFilter filtro = new FileNameExtensionFilter("Imágenes (JPG, PNG, GIF)", "jpg", "png", "gif");
        buscador.setFileFilter(filtro);
        
        int resultado = buscador.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File archivo = buscador.getSelectedFile();
            if (jugador == 1) skin1 = new ImageIcon(archivo.getAbsolutePath()).getImage();
            else skin2 = new ImageIcon(archivo.getAbsolutePath()).getImage();
        }
    }

    private void configurarArmas() {
        if (seleccion1 == 0) { dmg1 = 12; cooldown1 = 400; velMov1 = 5; } 
        else if (seleccion1 == 1) { dmg1 = 25; cooldown1 = 900; velMov1 = 3; } 
        else if (seleccion1 == 2) { dmg1 = 7; cooldown1 = 180; velMov1 = 8; } 

        if (seleccion2 == 0) { dmg2 = 12; cooldown2 = 400; velMov2 = 5; }
        else if (seleccion2 == 1) { dmg2 = 25; cooldown2 = 900; velMov2 = 3; }
        else if (seleccion2 == 2) { dmg2 = 7; cooldown2 = 180; velMov2 = 8; }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (estadoActual == Estado.MENU) {
            dibujarMenu(g);
        } else {
            g.setColor(new Color(20, 20, 20));
            g.fillRect(0, 0, 1200, 900);
            
            g.setColor(Color.DARK_GRAY);
            for(Rectangle r : coberturas) g.fillRect(r.x, r.y, r.width, r.height);
            
            if (powerUp != null) {
                g.setColor(tipoPowerUp.equals("VAMPIRO") ? Color.RED : (tipoPowerUp.equals("ESCUDO") ? Color.CYAN : Color.MAGENTA));
                g.fillOval(powerUp.x, powerUp.y, 25, 25);
                g.setColor(Color.WHITE); g.drawString(tipoPowerUp, powerUp.x - 10, powerUp.y - 5);
            }

            dibujarJugador(g2, x1, y1, skin1, rotacion1, timerEscudo1 > 0, Color.CYAN, Color.BLUE);
            dibujarHUD(g, x1, y1, vida1, armadura1, "J1" + (ia1 ? " (BOT)" : ""));
            
            dibujarJugador(g2, x2, y2, skin2, rotacion2, timerEscudo2 > 0, Color.YELLOW, Color.RED);
            dibujarHUD(g, x2, y2, vida2, armadura2, "J2" + (ia2 ? " (BOT)" : ""));

            g.setColor(Color.CYAN); for (Rectangle b : balas1) g.fillRect(b.x, b.y, 10, 5);
            g.setColor(Color.ORANGE); for (Rectangle b : balas2) g.fillRect(b.x, b.y, 10, 5);
        }
    }

    private void dibujarMenu(Graphics g) {
        g.setColor(new Color(30, 30, 30)); g.fillRect(0, 0, 1200, 900);
        g.setColor(Color.WHITE); g.setFont(new Font("Arial", Font.BOLD, 40));
        g.drawString("DUELO 2D - SELECCIÓN", 380, 100);
        
        g.setFont(new Font("Monospaced", Font.PLAIN, 18));
        g.setColor(Color.CYAN);
        g.drawString("JUGADOR 1:", 100, 250);
        g.drawString("Clase: " + armas[seleccion1], 100, 280);
        g.drawString("Control: " + (ia1 ? "IA" : "HUMANO"), 100, 310);
        g.drawString("(W: Cambiar | Q: IA | C: SKIN)", 100, 340);
        
        g.setColor(Color.ORANGE);
        g.drawString("JUGADOR 2:", 800, 250);
        g.drawString("Clase: " + armas[seleccion2], 800, 280);
        g.drawString("Control: " + (ia2 ? "IA" : "HUMANO"), 800, 310);
        g.drawString("(UP: Cambiar | M: IA | N: SKIN)", 800, 340);
        
        g.setColor(Color.YELLOW); 
        g.setFont(new Font("Arial", Font.BOLD, 25));
        g.drawString("PRESIONA ENTER PARA LUCHAR", 400, 500);
    }

    private void dibujarJugador(Graphics2D g2, int x, int y, Image img, double a, boolean esc, Color ce, Color defecto) {
        AffineTransform viejo = g2.getTransform();
        g2.translate(x + 20, y + 20);
        g2.rotate(a);

        if (img != null) {
            g2.drawImage(img, -20, -20, 40, 40, null);
        } else {
            g2.setColor(defecto);
            g2.fillRect(-20, -20, 40, 40);
            g2.setColor(Color.WHITE);
            g2.drawRect(-20, -20, 40, 40);
        }

        if (esc) {
            g2.setColor(ce);
            g2.setStroke(new BasicStroke(3));
            g2.drawOval(-25, -25, 50, 50);
        }
        g2.setTransform(viejo);
    }

    private void dibujarHUD(Graphics g, int x, int y, int v, int a, String n) {
        g.setColor(Color.BLACK); g.fillRect(x, y-30, 42, 12);
        g.setColor(Color.RED); g.fillRect(x+1, y-29, 40, 5);
        g.setColor(Color.GREEN); g.fillRect(x+1, y-29, Math.max(0, (int)(v * 0.2)), 5);
        g.setColor(Color.BLUE); g.fillRect(x+1, y-23, Math.max(0, (int)(a * 0.5)), 4);
        g.setColor(Color.WHITE); g.setFont(new Font("Arial", Font.PLAIN, 10));
        g.drawString(n, x, y-35);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (estadoActual == Estado.JUEGO) {
            if (vida1 <= 0 || vida2 <= 0) {
                timer.stop();
                JOptionPane.showMessageDialog(this, (vida1 <= 0 ? "JUGADOR 2" : "JUGADOR 1") + " GANA!");
                return;
            }
            procesarIA();
            actualizarMovimiento();
            rotacion1 += 0.05; rotacion2 += 0.05;
            
            x1 = (x1 + 1200) % 1200; y1 = (y1 + 900) % 900;
            x2 = (x2 + 1200) % 1200; y2 = (y2 + 900) % 900;
            
            if (timerEscudo1 > 0) timerEscudo1--; if (timerRafaga1 > 0) timerRafaga1--; if (timerVampiro1 > 0) timerVampiro1--;
            if (timerEscudo2 > 0) timerEscudo2--; if (timerRafaga2 > 0) timerRafaga2--; if (timerVampiro2 > 0) timerVampiro2--;
            
            if (powerUp == null && ++powerUpTimer > 400) {
                int prob = rnd.nextInt(100);
                tipoPowerUp = (prob < 15) ? "VAMPIRO" : (prob < 55 ? "ESCUDO" : "RAFAGA");
                powerUp = new Rectangle(300+rnd.nextInt(600), 100+rnd.nextInt(600), 25, 25);
                powerUpTimer = 0;
            }
            
            if (regenTimer1 > 0) regenTimer1--; else if (armadura1 < MAX_ARMADURA) armadura1++;
            if (regenTimer2 > 0) regenTimer2--; else if (armadura2 < MAX_ARMADURA) armadura2++;
            chequearColisiones();
        }
        repaint();
    }

    private void procesarIA() {
        if (ia1) controlarBot(1, x2, y2, balas2);
        if (ia2) controlarBot(2, x1, y1, balas1);
    }

    private void controlarBot(int id, int tX, int tY, ArrayList<Rectangle> bEnemigas) {
        int curX = (id == 1) ? x1 : x2;
        int curY = (id == 1) ? y1 : y2;

        for (Rectangle b : bEnemigas) {
            if (Math.abs(b.y - curY) < 50 && Math.abs(b.x - curX) < 200) {
                mover(id, curY > 100 ? "UP" : "DOWN"); return;
            }
        }
        if (powerUp != null) {
            mover(id, powerUp.y < curY ? "UP" : "DOWN");
            mover(id, powerUp.x < curX ? "LEFT" : "RIGHT");
            return;
        }
        if (Math.abs(curY - tY) > 10) mover(id, curY < tY ? "DOWN" : "UP");
        else { detenerY(id); disparar(id); }
        
        int dist = Math.abs(curX - tX);
        if (dist > 300) mover(id, curX < tX ? "RIGHT" : "LEFT");
        else if (dist < 200) mover(id, curX < tX ? "LEFT" : "RIGHT");
        else detenerX(id);
    }

    private void mover(int id, String d) {
        if(id==1){ 
            if(d.equals("UP")){up1=true;down1=false;} if(d.equals("DOWN")){down1=true;up1=false;}
            if(d.equals("LEFT")){left1=true;right1=false;} if(d.equals("RIGHT")){right1=true;left1=false;}
        } else {
            if(d.equals("UP")){up2=true;down2=false;} if(d.equals("DOWN")){down2=true;up2=false;}
            if(d.equals("LEFT")){left2=true;right2=false;} if(d.equals("RIGHT")){right2=true;left2=false;}
        }
    }
    private void detenerX(int id){ if(id==1){left1=false;right1=false;}else{left2=false;right2=false;} }
    private void detenerY(int id){ if(id==1){up1=false;down1=false;}else{up2=false;down2=false;} }

    private void actualizarMovimiento() {
        if (up1) y1 -= velMov1; if (down1) y1 += velMov1; if (left1) x1 -= velMov1; if (right1) x1 += velMov1;
        if (up2) y2 -= velMov2; if (down2) y2 += velMov2; if (left2) x2 -= velMov2; if (right2) x2 += velMov2;
    }

    private void chequearColisiones() {
        Rectangle r1 = new Rectangle(x1, y1, 40, 40);
        Rectangle r2 = new Rectangle(x2, y2, 40, 40);

        for (int i = 0; i < balas1.size(); i++) {
            Rectangle b = balas1.get(i); b.x += 15;
            boolean choca = false;
            for(Rectangle c : coberturas) if(b.intersects(c)) choca = true;
            if (b.intersects(r2)) { recibirDanio(2, dmg1); if(timerVampiro1 > 0) vida1 = Math.min(200, vida1 + (dmg1/2)); choca = true; }
            if (choca || b.x > 1200) { balas1.remove(i--); }
        }
        for (int i = 0; i < balas2.size(); i++) {
            Rectangle b = balas2.get(i); b.x -= 15;
            boolean choca = false;
            for(Rectangle c : coberturas) if(b.intersects(c)) choca = true;
            if (b.intersects(r1)) { recibirDanio(1, dmg2); if(timerVampiro2 > 0) vida2 = Math.min(200, vida2 + (dmg2/2)); choca = true; }
            if (choca || b.x < 0) { balas2.remove(i--); }
        }
        if (powerUp != null) {
            if (r1.intersects(powerUp)) { aplicarPowerUp(1); powerUp = null; }
            else if (r2.intersects(powerUp)) { aplicarPowerUp(2); powerUp = null; }
        }
    }

    private void aplicarPowerUp(int j) {
        if (j == 1) {
            if(tipoPowerUp.equals("ESCUDO")) timerEscudo1 = 300;
            if(tipoPowerUp.equals("RAFAGA")) timerRafaga1 = 300;
            if(tipoPowerUp.equals("VAMPIRO")) timerVampiro1 = 180;
        } else {
            if(tipoPowerUp.equals("ESCUDO")) timerEscudo2 = 300;
            if(tipoPowerUp.equals("RAFAGA")) timerRafaga2 = 300;
            if(tipoPowerUp.equals("VAMPIRO")) timerVampiro2 = 180;
        }
    }

    private void recibirDanio(int j, int d) {
        if (j == 1) {
            if (timerEscudo1 > 0) return;
            regenTimer1 = ESPERA_REGEN;
            if (armadura1 > 0) { armadura1 -= d; if (armadura1 < 0) { vida1 += armadura1; armadura1 = 0; } } else vida1 -= d;
        } else {
            if (timerEscudo2 > 0) return;
            regenTimer2 = ESPERA_REGEN;
            if (armadura2 > 0) { armadura2 -= d; if (armadura2 < 0) { vida2 += armadura2; armadura2 = 0; } } else vida2 -= d;
        }
    }

    private void disparar(int j) {
        long ahora = System.currentTimeMillis();
        if (j == 1) {
            long cd = (timerRafaga1 > 0) ? cooldown1 / 2 : cooldown1;
            if (ahora - lastShot1 > cd) { balas1.add(new Rectangle(x1 + 40, y1 + 18, 10, 5)); lastShot1 = ahora; }
        } else {
            long cd = (timerRafaga2 > 0) ? cooldown2 / 2 : cooldown2;
            if (ahora - lastShot2 > cd) { balas2.add(new Rectangle(x2 - 10, y2 + 18, 10, 5)); lastShot2 = ahora; }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (estadoActual == Estado.MENU) {
            if (k == KeyEvent.VK_W) seleccion1 = (seleccion1 + 1) % 3;
            if (k == KeyEvent.VK_Q) ia1 = !ia1;
            if (k == KeyEvent.VK_C) seleccionarSkin(1);
            
            if (k == KeyEvent.VK_UP) seleccion2 = (seleccion2 + 1) % 3;
            if (k == KeyEvent.VK_M) ia2 = !ia2;
            if (k == KeyEvent.VK_N) seleccionarSkin(2);
            
            if (k == KeyEvent.VK_ENTER) { configurarArmas(); estadoActual = Estado.JUEGO; }
        } else {
            if (!ia1) {
                if (k == KeyEvent.VK_W) up1 = true; if (k == KeyEvent.VK_S) down1 = true;
                if (k == KeyEvent.VK_A) left1 = true; if (k == KeyEvent.VK_D) right1 = true;
                if (k == KeyEvent.VK_SPACE) disparar(1);
            }
            if (!ia2) {
                if (k == KeyEvent.VK_UP) up2 = true; if (k == KeyEvent.VK_DOWN) down2 = true;
                if (k == KeyEvent.VK_LEFT) left2 = true; if (k == KeyEvent.VK_RIGHT) right2 = true;
                if (k == KeyEvent.VK_ENTER) disparar(2);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_W) up1 = false; if (k == KeyEvent.VK_S) down1 = false;
        if (k == KeyEvent.VK_A) left1 = false; if (k == KeyEvent.VK_D) right1 = false;
        if (k == KeyEvent.VK_UP) up2 = false; if (k == KeyEvent.VK_DOWN) down2 = false;
        if (k == KeyEvent.VK_LEFT) left2 = false; if (k == KeyEvent.VK_RIGHT) right2 = false;
    }

    @Override public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {
        JFrame f = new JFrame("Duelo 2D - Personalizable");
        Duelo2D juego = new Duelo2D();
        f.add(juego); 
        f.setSize(1200, 900);
        f.setResizable(false);
        f.setLocationRelativeTo(null);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setVisible(true);
    }
}