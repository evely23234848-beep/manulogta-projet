package manulogta;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Manulogta {
    public static void main(String[] args) {
        ProyectoMedidor medidor = new ProyectoMedidor();
        Scanner sc = new Scanner(System.in);
        
        while (true) {
            medidor.menu();
            System.out.print("Opcion: ");
            String op = sc.nextLine();
            
            if (op.equals("1")) medidor.iniciarMedicion();
            else if (op.equals("2")) medidor.verEstadisticasHora();
            else if (op.equals("3")) {
                try {
                    System.out.print("Nuevo nivel de alerta: ");
                    medidor.setNivelAlerta(Integer.parseInt(sc.nextLine()));
                } catch (NumberFormatException e) {
                    System.out.println("Error: Ingresa un numero valido.");
                }
            }
            else if (op.equals("4")) medidor.mostrarGrafica();
            else if (op.equals("5")) {
                System.out.println("Saliendo del programa...");
                System.exit(0);
            }
        }
    }
}

class ProyectoMedidor {
    private int nivelCo2;
    private ArrayList<Integer> datosGrafica = new ArrayList<>();
    private ArrayList<String> historial;
    private int nivelAlerta;
    private Random random;
    private Scanner scanner;
    private VisualizadorParticulas ventanaVisual;
    private boolean ventilando = false;

    public ProyectoMedidor() {
        random = new Random();
        nivelCo2 = 500 + random.nextInt(101);
        historial = new ArrayList<>();
        nivelAlerta = 1000;
        scanner = new Scanner(System.in);
    }

    public void setNivelAlerta(int alerta) { this.nivelAlerta = alerta; }

    public void activarVentilacion() {
        ventilando = true;
        System.out.println("\n--- SISTEMA Ventilando ---");
        try { Thread.sleep(1500); } catch (InterruptedException e) {} 
        nivelCo2 = 450 + random.nextInt(50);
        ventilando = false;
        System.out.println("Nivel de CO2 bajo a: " + nivelCo2 + " ppm\n");
    }

    public String obtenerCalidadAire() {
        if (nivelCo2 < 600) return "Excelente";
        if (nivelCo2 < 800) return "Aceptable";
        if (nivelCo2 < 1000) return "Malo";
        return "CRITICO";
    }

    public void iniciarMedicion() {
        if (ventanaVisual == null) {
            ventanaVisual = new VisualizadorParticulas();
        }
        ventanaVisual.setVisible(true);

        Thread hiloLectura = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    if (!ventilando) nivelCo2 += random.nextInt(15);
                    
                    String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    String calidad = obtenerCalidadAire();
                    historial.add("[" + hora + "] CO2: " + nivelCo2 + " ppm | " + calidad);
                    datosGrafica.add(nivelCo2);
                    
                    ventanaVisual.actualizarDatos(nivelCo2, calidad, ventilando);
                    
                    System.out.println("[" + hora + "] CO2: " + nivelCo2 + " ppm | " + calidad);

                    if (nivelCo2 > nivelAlerta) {
                        System.out.println("ALERTA ALTA: " + nivelCo2 + " ppm");
                    }
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) { }
        });

        hiloLectura.start();

        System.out.println("\nMonitor Activo. Presiona ENTER para ventilar. Escribe 'salir' para detener.");
        while (true) {
            String entrada = scanner.nextLine();
            if (entrada.equalsIgnoreCase("salir")) {
                hiloLectura.interrupt();
                ventanaVisual.setVisible(false);
                break;
            } else {
                activarVentilacion();
            }
        }
    }

    public void verEstadisticasHora() {
        System.out.println("\n--- Historial ---");
        for (String r : historial) System.out.println(r);
    }

    public void mostrarGrafica() {
        if (datosGrafica.isEmpty()) {
            System.out.println("No hay datos para graficar aun.");
            return;
        }
        JFrame frame = new JFrame("Grafica de CO2 en el Tiempo");
        frame.setSize(800, 400);
        frame.add(new PanelGrafico(datosGrafica));
        frame.setVisible(true);
    }

    public void menu() {
        System.out.println("\nSISTEMA CO2 - PROYECTO MANULOGTA");
        System.out.println("1. Monitoreo visual y tiempo real");
        System.out.println("2. Ver historial");
        System.out.println("3. Configurar alerta");
        System.out.println("4. Ver grafica de evolucion");
        System.out.println("5. Salir");
    }
}

class PanelGrafico extends JPanel {
    private ArrayList<Integer> datos;

    public PanelGrafico(ArrayList<Integer> datos) {
        this.datos = new ArrayList<>(datos);
        setBackground(Color.BLACK);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int padding = 50;
        int width = getWidth() - 2 * padding;
        int height = getHeight() - 2 * padding;

        g2.setColor(Color.WHITE);
        g2.drawLine(padding, getHeight() - padding, padding, padding); 
        g2.drawLine(padding, getHeight() - padding, getWidth() - padding, getHeight() - padding); 

        // Dibujar numeros ppm en el eje izquierdo
        for (int i = 0; i <= 2000; i += 400) {
            int y = (getHeight() - padding) - (int) (i * (height / 2000.0));
            g2.drawString(i + " ppm", 5, y);
            g2.drawLine(padding - 5, y, padding, y);
        }

        if (datos.size() < 2) return;

        double xScale = (double) width / (datos.size() - 1);
        double yScale = (double) height / 2000.0; 

        g2.setColor(Color.CYAN);
        for (int i = 0; i < datos.size() - 1; i++) {
            int x1 = padding + (int) (i * xScale);
            int y1 = (getHeight() - padding) - (int) (datos.get(i) * yScale);
            int x2 = padding + (int) ((i + 1) * xScale);
            int y2 = (getHeight() - padding) - (int) (datos.get(i + 1) * yScale);
            g2.drawLine(x1, y1, x2, y2);
            g2.fillOval(x1 - 2, y1 - 2, 4, 4);
        }
        g2.drawString("Evolucion de PPM", getWidth()/2 - 50, 30);
    }
}

class VisualizadorParticulas extends JFrame {
    private int co2;
    private String estado = "Bueno";
    private boolean ventilando = false;
    private ArrayList<Particula> particulas = new ArrayList<>();

    public VisualizadorParticulas() {
        setTitle("Simulador Visual CO2 - Empresa X");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        for(int i=0; i<60; i++) particulas.add(new Particula());
        
        Timer timer = new Timer(30, e -> {
            for(Particula p : particulas) p.mover(ventilando);
            repaint();
        });
        timer.start();
    }

    public void actualizarDatos(int co2, String estado, boolean ventilando) {
        this.co2 = co2;
        this.estado = estado;
        this.ventilando = ventilando;
    }

    @Override
    public void paint(Graphics g) {
        Image dbImage = createImage(getWidth(), getHeight());
        Graphics dbg = dbImage.getGraphics();
        
        dbg.setColor(new Color(30, 30, 30));
        dbg.fillRect(0, 0, getWidth(), getHeight());

        Color colorP = Color.GREEN;
        if (estado.equals("Aceptable")) colorP = Color.YELLOW;
        if (estado.equals("Malo")) colorP = Color.ORANGE;
        if (estado.equals("CRITICO")) colorP = Color.RED;

        dbg.setColor(colorP);
        for(Particula p : particulas) {
            dbg.fillOval(p.x, p.y, 8, 8);
        }

        dbg.setColor(Color.WHITE);
        dbg.setFont(new Font("Arial", Font.BOLD, 18));
        dbg.drawString("CO2: " + co2 + " ppm", 20, 60);
        dbg.drawString("Estado: " + estado, 20, 90);
        if(ventilando) {
            dbg.setColor(Color.CYAN);
            dbg.drawString("VENTILANDO - Aire saliendo...", 20, 120);
            dbg.drawRect(580, 100, 10, 200);
        }

        g.drawImage(dbImage, 0, 0, this);
    }
}

class Particula {
    int x = new Random().nextInt(550), y = new Random().nextInt(300) + 50;
    int vx = new Random().nextInt(5) - 2, vy = new Random().nextInt(5) - 2;

    void mover(boolean ventilando) {
        if (ventilando) {
            x += 10;
            if (x > 600) x = -10;
        } else {
            x += vx; y += vy;
            if (x<0 || x>580) vx *= -1;
            if (y<50 || y>380) vy *= -1;
        }
    }
}