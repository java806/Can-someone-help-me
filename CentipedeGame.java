import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class CentipedeGame extends JPanel {
    private int width;
    private int height;

    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean spacePressed = false;

    private Player player;
    private List<Bullet> bullets;
    private List<Mushroom> mushrooms;
    private Centipede centipede;
    private Spider spider;
    private Turtle turtle;
    private Ghost ghost;

    private final int FIRE_DELAY = 200;
    private int score = 0;
    private int lives = 3;
    private int highscore = 0;
    private int level = 1;
    private int centipededeaths = 1;
    private int segmentcount = 10;

    public CentipedeGame(int width, int height) {
        this.width = width;
        this.height = height;
        setPreferredSize(new Dimension(width, height));
        setBackground(Color.BLACK);

        player = new Player(width / 2, height - 50);
        bullets = new ArrayList<>();
        mushrooms = new ArrayList<>();
        centipede = new Centipede(width, height);
        spider = new Spider(width, height);
        turtle = new Turtle(width);
        ghost = new Ghost(width, height);

        generateMushrooms();

        level = centipededeaths;

        if (score > highscore) {
            highscore = score;
        }

        if (segmentcount == 0) {
            centipede.respawn();
            centipededeaths += 1;
        }

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT -> leftPressed = true;
                    case KeyEvent.VK_RIGHT -> rightPressed = true;
                    case KeyEvent.VK_UP -> upPressed = true;
                    case KeyEvent.VK_DOWN -> downPressed = true;
                    case KeyEvent.VK_SPACE -> spacePressed = true;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT -> leftPressed = false;
                    case KeyEvent.VK_RIGHT -> rightPressed = false;
                    case KeyEvent.VK_UP -> upPressed = false;
                    case KeyEvent.VK_DOWN -> downPressed = false;
                    case KeyEvent.VK_SPACE -> spacePressed = false;
                }
            }
        });
        setFocusable(true);
        requestFocusInWindow();
    }

    private void generateMushrooms() {
        Random random = new Random();
        int numMushroomsToAdd = 25;
        for (int i = 0; i < numMushroomsToAdd; i++) {
            int x = random.nextInt(width - 20);
            int y = random.nextInt(height - 20);
            mushrooms.add(new Mushroom(x, y));
        }
    }

    private void start() {
        boolean running = true;
        long lastTime = System.nanoTime();
        final double nsPerTick = 1000000000.0 / 60.0;
        double delta = 0;
        long lastFireTime = 0;

        while (running) {
            long now = System.nanoTime();
            delta += (now - lastTime) / nsPerTick;
            lastTime = now;

            while (delta >= 1) {
                update();
                delta -= 1;
            }

            if (spacePressed && (System.currentTimeMillis() - lastFireTime) >= FIRE_DELAY) {
                bullets.add(new Bullet(player.getX() + player.getWidth() / 2, player.getY()));
                lastFireTime = System.currentTimeMillis();
            }

            repaint();

            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {
        player.update(leftPressed, rightPressed, upPressed, downPressed, width, height);
        spider.update(width, height);
        turtle.update(height);
        ghost.update(width);
        centipede.update(mushrooms);
    
        Rectangle playerBounds = new Rectangle(player.getX(), player.getY(), player.getWidth(), player.getHeight());
        if (centipede.intersects(playerBounds)) {
            lives--;
            System.out.println("Lives: " + lives);
            if (lives <= 0) {
                System.out.println("Game Over!");
                System.exit(0);
            }
        }
    
        if (spider.isVisible() && spider.intersects(playerBounds)) {
            lives--;
            System.out.println("Lives: " + lives);
            if (lives <= 0) {
                System.out.println("Game Over!");
                System.exit(0);
            }
        }
    
        if (turtle.isVisible() && turtle.intersects(playerBounds)) {
            lives--;
            System.out.println("Lives: " + lives);
            if (lives <= 0) {
                System.out.println("Game Over!");
                System.exit(0);
            }
        }
    
        Iterator<Bullet> bulletIterator = bullets.iterator();
        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();
            bullet.update();
    
            Rectangle bulletBounds = new Rectangle(bullet.getX(), bullet.getY(), bullet.getWidth(), bullet.getHeight());
    
            if (spider.isVisible() && spider.intersects(bulletBounds)) {
                bulletIterator.remove();
                score += 300;
                spider.disappearAndReappear(width, height);
                continue;
            }
    
            if (turtle.isVisible() && turtle.intersects(bulletBounds)) {
                bulletIterator.remove();
                score += 100;
                turtle.disappearAndReappear(width);
                continue;
            }
    
            if (ghost.isVisible() && ghost.intersects(bulletBounds)) {
                bulletIterator.remove();
                score += 500;
                ghost.disappearAndReappear(width, height);
                continue;
            }
    
            Iterator<Mushroom> mushroomIterator = mushrooms.iterator();
            while (mushroomIterator.hasNext()) {
                Mushroom mushroom = mushroomIterator.next();
                Rectangle mushroomBounds = mushroom.getBounds();
                if (bulletBounds.intersects(mushroomBounds)) {
                    bulletIterator.remove();
                    mushroomIterator.remove();
                    score += 50;
                    break;
                }
            }
    
            Iterator<Centipede.Segment> segmentIterator = centipede.getSegments().iterator();
            while (segmentIterator.hasNext()) {
                Centipede.Segment segment = segmentIterator.next();
                if (bulletBounds.intersects(segment.getBounds())) {
                    bulletIterator.remove();
                    segmentIterator.remove();
                    score += 200;
                    segmentcount -= 1;
                    break;
                }
            }
    
            if (bullet.getY() < 0) {
                bulletIterator.remove();
            }
        }
        bullets.removeIf(b -> b.getY() < 0);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        player.draw(g);
        centipede.draw(g);
    
        for (Bullet bullet : bullets) {
            bullet.draw(g);
        }
    
        for (Mushroom mushroom : mushrooms) {
            mushroom.draw(g);
        }
    
        if (spider.isVisible()) {
            spider.draw(g);
        }
    
        if (turtle.isVisible()) {
            turtle.draw(g);
        }
    
        if (ghost.isVisible()) {
            ghost.draw(g);
        }
    
        g.setColor(Color.WHITE);
        g.drawString("Score: " + score, 10, 20);
        g.drawString("High Score: " + highscore, 10, 40);
        g.drawString("Lives: " + lives, 10, 60);
        g.drawString("Level: " + level, 10, 80);
    
        System.out.println("Score: " + score);
        System.out.println("Lives: " + lives);
    }

    static class Player {
        private int x, y;
        private final int width, height;

        public Player(int x, int y) {
            this.x = x;
            this.y = y;
            this.width = 30;
            this.height = 30;
        }

        public void update(boolean leftPressed, boolean rightPressed, boolean upPressed, boolean downPressed, int gameWidth, int gameHeight) {
            if (leftPressed) x = Math.max(0, x - 5);
            if (rightPressed) x = Math.min(1537 - width, x + 5);
            if (upPressed) y = Math.max(500, y - 5);
            if (downPressed) y = Math.min(gameHeight - 317, y + 5);
        }

        public void draw(Graphics g) {
            g.setColor(Color.YELLOW);
            g.fillRect(x, y, width, height);
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    static class Bullet {
        private int x, y;
        private final int width = 5;
        private final int height = 20;

        public Bullet(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void update() {
            y -= 25;
        }

        public void draw(Graphics g) {
            g.setColor(Color.YELLOW);
            g.fillRect(x, y, width, height);
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    static class Mushroom {
        private int x, y;
        private final int width = 20;
        private final int height = 20;

        public Mushroom(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public void draw(Graphics g) {
            g.setColor(Color.ORANGE);
            g.fillRect(x, y, width, height);
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }

    static class Centipede {
        private List<Segment> segments;
        private final int gameWidth;
        private final int gameHeight;

        public Centipede(int gameWidth, int gameHeight) {
            this.gameWidth = gameWidth;
            this.gameHeight = gameHeight;
            spawn();
        }

        public void update(List<Mushroom> mushrooms) {
            for (Segment segment : segments) {
                segment.update(mushrooms, gameWidth, gameHeight);
            }
        }

        public void draw(Graphics g) {
            for (Segment segment : segments) {
                segment.draw(g);
            }
        }

        public void spawn() {
            segments = new ArrayList<>();
            int startX = gameWidth / 2;
            int startY = 0;
            for (int i = 0; i < 10; i++) {
                segments.add(new Segment(startX - i * 20, startY));
            }
        }

        public void respawn() {
                segments = new ArrayList<>();
                int startX = gameWidth / 2;
                int startY = 0;
                for (int i = 0; i < 10; i++) {
                    segments.add(new Segment(startX - i * 20, startY));
                }
        }

        public List<Segment> getSegments() {
            return segments;
        }

        public Rectangle getBounds() {
            if (segments.isEmpty()) {
                return new Rectangle(0, 0, 0, 0);
            } else {
                Segment head = segments.get(0);
                return new Rectangle(head.getX(), head.getY(), head.getWidth(), head.getHeight());
            }
        }

        public boolean intersects(Rectangle other) {
            for (Segment segment : segments) {
                if (segment.getBounds().intersects(other)) {
                    return true;
                }
            }
            return false;
        }

        public void hit() {
            segments.remove(0);
            if (segments.isEmpty()) {
            }
        }

        static class Segment {
            private int x, y;
            private final int width = 20;
            private final int height = 20;
            private int direction = 1;

            public Segment(int x, int y) {
                this.x = x;
                this.y = y;
            }

            public void update(List<Mushroom> mushrooms, int gameWidth, int gameHeight) {
                x += direction * 2;

                for (Mushroom mushroom : mushrooms) {
                    Rectangle mushroomBounds = mushroom.getBounds();
                    if (getBounds().intersects(mushroomBounds)) {
                        y += height;
                        direction *= -1;
                        break;
                    }
                }

                if (x < 0 || x > 1545 - width) {
                    direction *= -1;
                    y += height;
                }

                if (y > gameHeight - height) {
                    y = gameHeight - height;
                }
            }

            public void draw(Graphics g) {
                g.setColor(Color.GREEN);
                g.fillRect(x, y, width, height);
            }

            public int getX() {
                return x;
            }

            public int getY() {
                return y;
            }

            public int getWidth() {
                return width;
            }

            public int getHeight() {
                return height;
            }

            public Rectangle getBounds() {
                return new Rectangle(x, y, width, height);
            }
        }
    }

    static class Spider {
        private int x, y;
        private final int width = 40;
        private final int height = 40;
        private boolean visible;
        private Random random;
        private Timer timer;
        private long lastMoveTime;
        private final int MOVE_INTERVAL = 150;
        private final int MOVE_DISTANCE = 50;
        private final int REAPPEAR_DELAY = 10000;

        public Spider(int gameWidth, int gameHeight) {
            random = new Random();
            this.x = random.nextInt(gameWidth - width);
            this.y = random.nextInt(gameHeight - height);
            this.visible = true;
            timer = new Timer();
            lastMoveTime = System.currentTimeMillis();
        }

        public void update(int gameWidth, int gameHeight) {
            if (visible) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastMoveTime >= MOVE_INTERVAL) {
                    lastMoveTime = currentTime;

                    int moveX = random.nextInt(3) - 1;
                    int moveY = random.nextInt(3) - 1;

                    x += moveX * MOVE_DISTANCE;
                    y += moveY * MOVE_DISTANCE;

                    x = Math.max(0, Math.min(1545 - width, x));
                    y = Math.max(0, Math.min(317 - height, y));
                }
            }
        }

        public boolean intersects(Rectangle rect) {
            Rectangle spiderBounds = new Rectangle(x, y, width, height);
            return spiderBounds.intersects(rect);
        }

        public void draw(Graphics g) {
            if (visible) {
                g.setColor(Color.BLUE);
                g.fillRect(x, y, width, height);
            }
        }

        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        public void disappearAndReappear(int gameWidth, int gameHeight) {
            if (visible) {
                setVisible(false);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        x = random.nextInt(gameWidth - width);
                        y = random.nextInt(gameHeight - height);
                        setVisible(true);
                    }
                }, REAPPEAR_DELAY);
            }
        }
    }
    static class Turtle {
        private int x, y;
        private final int width = 30;
        private final int height = 30;
        private boolean visible;
        private Random random;
        private Timer timer;
        @SuppressWarnings("unused")
        private final int MOVE_INTERVAL = 10;
        private final int MOVE_DISTANCE = 4;
        private final int REAPPEAR_DELAY = 10000;

        public Turtle(int gameWidth) {
            random = new Random();
            this.x = random.nextInt(gameWidth - width);
            this.y = 0 - height;
            this.visible = true;
            timer = new Timer();
        }

        public void update(int gameHeight) {
            if (visible) {
                y += MOVE_DISTANCE;
                if (y > gameHeight) {
                    setVisible(false);
                    disappearAndReappear(gameHeight);
                }
            }
        }

        public boolean intersects(Rectangle rect) {
            Rectangle turtleBounds = new Rectangle(x, y, width, height);
            return turtleBounds.intersects(rect);
        }

        public void draw(Graphics g) {
            if (visible) {
                g.setColor(Color.RED);
                g.fillRect(x, y, width, height);
            }
        }

        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        public void disappearAndReappear(int gameWidth) {
            setVisible(false);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    x = random.nextInt(gameWidth - width);
                    y = 0 - height;
                    setVisible(true);
                }
            }, REAPPEAR_DELAY);
        }
    }

    static class Ghost {
        private int x, y;
        private final int width = 40;
        private final int height = 40;
        private boolean visible;
        private Random random;
        private Timer timer;
        private long lastMoveTime;
        private final int MOVE_INTERVAL = 10;
        private final int MOVE_DISTANCE = 4;
        private final int REAPPEAR_DELAY = 60000;
        private int direction = 1;

        public Ghost(int gameWidth, int gameHeight) {
            random = new Random();
            this.x = random.nextInt(gameWidth - width);
            this.y = random.nextInt(gameHeight / 2 - height);
            this.visible = true;
            timer = new Timer();
            lastMoveTime = System.currentTimeMillis();
        }

        public void update(int gameWidth) {
            if (visible) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastMoveTime >= MOVE_INTERVAL) {
                    lastMoveTime = currentTime;
                    x += direction * MOVE_DISTANCE;
                    if (x < 0 || x > gameWidth - width) {
                        direction *= -1;
                    }
                }
            }
        }

        public boolean intersects(Rectangle rect) {
            Rectangle ghostBounds = new Rectangle(x, y, width, height);
            return ghostBounds.intersects(rect);
        }

        public void draw(Graphics g) {
            if (visible) {
                g.setColor(Color.CYAN);
                g.fillRect(x, y, width, height);
            }
        }

        public boolean isVisible() {
            return visible;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        public void disappearAndReappear(int gameWidth, int gameHeight) {
            setVisible(false);
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    x = random.nextInt(gameWidth - width);
                    y = random.nextInt(gameHeight / 2 - height);
                    setVisible(true);
                }
            }, REAPPEAR_DELAY);
        }
    }
    public static void main(String[] args) {
        JFrame frame = new JFrame("Centipede Game");
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int width = gd.getDisplayMode().getWidth();
        int height = gd.getDisplayMode().getHeight();
        CentipedeGame game = new CentipedeGame(width, height);
        frame.add(game);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        game.start();
    }
}
