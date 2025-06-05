package pacman.games; // You can adjust this package name

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Random;

/**
 * Main class for the Pac-Man Game.
 * Sets up the JFrame and adds the GamePanel.
 */
public class PacManGame extends JFrame {

    public PacManGame() {
        // Create and set up the game panel
        GamePanel gamePanel = new GamePanel();
        this.add(gamePanel); // Add the game panel to the frame

        // Set frame properties
        this.setTitle("Pac-Man");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Close operation
        this.setResizable(false); // Prevent resizing
        this.pack(); // Sizes the frame so that all its contents are at or above their preferred sizes
        this.setLocationRelativeTo(null); // Center the window on the screen
        this.setVisible(true); // Make the frame visible
    }

    /**
     * Main method to start the Pac-Man Game.
     * @param args Command line arguments (not used).
     */
    public static void main(String[] args) {
        // Run the game on the Event Dispatch Thread (EDT) for Swing applications
        SwingUtilities.invokeLater(() -> {
            new PacManGame();
        });
    }
}

/**
 * Represents the Pac-Man character.
 */
class PacMan {
    int x, y; // Current grid coordinates
    int dx, dy; // Direction vector (e.g., dx=1, dy=0 for right)
    double mouthAngle = 0; // Starting mouth angle (0 for closed, opens up to 45)
    double mouthSpeed = 5; // Speed of mouth animation
    int lives;

    public PacMan(int startX, int startY, int initialLives) {
        this.x = startX;
        this.y = startY;
        this.dx = 1; // Initial direction: right
        this.dy = 0;
        this.lives = initialLives;
    }

    // Resets Pac-Man's position and direction after losing a life
    public void reset(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.dx = 1; // Reset direction to right
        this.dy = 0;
        this.mouthAngle = 0; // Reset mouth to closed
        this.mouthSpeed = 5;
    }

    public void move() {
        x += dx;
        y += dy;

        // Animate mouth: opens from 0 to 45, then closes back to 0
        mouthAngle += mouthSpeed;
        if (mouthAngle >= 45) { // If mouth is fully open
            mouthAngle = 45; // Cap it
            mouthSpeed *= -1; // Reverse to close
        } else if (mouthAngle <= 0) { // If mouth is fully closed
            mouthAngle = 0; // Cap it
            mouthSpeed *= -1; // Reverse to open
        }
    }
}

/**
 * Represents a Ghost character.
 */
class Ghost {
    int x, y; // Current grid coordinates
    int dx, dy; // Direction vector
    Color color; // Ghost color
    boolean frightened; // State when Pac-Man eats a power pellet
    long frightenedTimer; // Timer for frightened state

    public Ghost(int startX, int startY, Color color) {
        this.x = startX;
        this.y = startY;
        this.color = color;
        this.dx = 0; // Initial random direction
        this.dy = 0;
        this.frightened = false;
        this.frightenedTimer = 0;
    }

    // Resets Ghost's position and state
    public void reset(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.dx = 0;
        this.dy = 0;
        this.frightened = false;
        this.frightenedTimer = 0;
    }
}

/**
 * GamePanel handles all the game logic and drawing for Pac-Man.
 * It extends JPanel and implements ActionListener for game updates
 * and KeyListener for user input.
 */
class GamePanel extends JPanel implements ActionListener {

    // --- Game Constants ---
    private final int TILE_SIZE = 30; // Size of each grid tile (wall, pellet, pacman, ghost)
    private final int BOARD_ROWS = 21; // Number of rows in the maze
    private final int BOARD_COLS = 21; // Number of columns in the maze
    private final int SCREEN_WIDTH = BOARD_COLS * TILE_SIZE; // Width of the game screen
    private final int SCREEN_HEIGHT = BOARD_ROWS * TILE_SIZE + 50; // Height of the game screen (+50 for score/lives)
    private final int DELAY = 150; // Delay in milliseconds for game updates (controls speed)
    private final int POWER_PELLET_FRIGHTEN_TIME = 8000; // 8 seconds in milliseconds

    // --- Game State Variables ---
    private int[][] board; // Represents the maze: 0=empty, 1=wall, 2=pellet, 3=power pellet
    private PacMan pacMan;
    private ArrayList<Ghost> ghosts;
    private int score;
    private int pelletsRemaining;
    private Timer timer;
    private Random random;

    private boolean running = false;
    private boolean gameOver = false;
    private boolean gameWon = false;

    // Start positions for Pac-Man and Ghosts (grid coordinates)
    private final Point PACMAN_START = new Point(BOARD_COLS / 2 - 1, BOARD_ROWS / 2 + 3);
    private final Point GHOST_START_1 = new Point(BOARD_COLS / 2 - 2, BOARD_ROWS / 2 - 2);
    private final Point GHOST_START_2 = new Point(BOARD_COLS / 2 + 1, BOARD_ROWS / 2 - 2);
    // Removed GHOST_START_3 and GHOST_START_4 as they are no longer needed for two ghosts

    private JButton retryButton;

    // Maze layout (1 for wall, 0 for empty, 2 for pellet, 3 for power pellet)
    // This is a simplified maze for demonstration.
    private final int[][] initialBoard = {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,2,2,2,2,2,2,2,2,1,2,2,2,2,2,2,2,2,2,3,1},
            {1,2,1,1,1,1,1,2,1,1,1,2,1,1,1,1,1,2,1,1,1},
            {1,3,1,2,2,2,1,2,2,2,2,2,2,2,2,2,1,2,2,2,1},
            {1,2,2,2,1,2,1,1,2,1,1,1,1,1,1,2,1,2,1,2,1},
            {1,2,1,2,1,2,2,2,2,2,2,2,2,2,1,2,1,2,1,2,1},
            {1,2,1,1,1,1,2,1,1,1,1,1,1,2,1,2,1,2,1,2,1},
            {1,2,2,2,2,2,2,2,2,2,2,2,2,2,1,2,1,2,2,2,1},
            {1,1,1,1,2,1,1,2,1,1,1,1,1,2,1,2,1,1,2,1,1},
            {1,2,2,2,2,2,1,2,2,0,0,0,1,2,2,2,1,2,2,2,1}, // Ghost house entrance
            {1,1,1,1,1,2,1,1,1,0,0,0,1,1,1,2,1,1,2,1,1}, // Ghost house
            {1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1},
            {1,2,1,1,1,1,1,1,1,1,2,1,1,1,1,1,2,1,1,2,1},
            {1,2,1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1,2,1},
            {1,2,1,2,1,1,1,1,1,1,1,1,1,1,1,1,1,2,1,2,1},
            {1,2,1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,1,2,1},
            {1,2,1,1,1,2,1,1,1,1,1,1,1,1,2,1,1,1,1,2,1},
            {1,2,2,2,2,2,2,2,2,1,2,2,2,2,2,2,2,2,2,2,1},
            {1,1,1,1,1,1,1,2,1,1,1,2,1,1,1,1,1,2,1,1,1},
            {1,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,3,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
    };


    /**
     * Constructor for the GamePanel.
     * Initializes game components and starts the game.
     */
    public GamePanel() {
        random = new Random();
        this.setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT)); // Set panel size
        // Change background to BLUE to represent walls
        this.setBackground(Color.BLUE);
        this.setFocusable(true); // Make panel focusable to receive key events
        this.addKeyListener(new MyKeyAdapter()); // Add key listener for user input

        // Initialize and configure the retry button
        retryButton = new JButton("Retry");
        retryButton.setFont(new Font("Ink Free", Font.BOLD, 30));
        retryButton.setBackground(Color.DARK_GRAY);
        retryButton.setForeground(Color.WHITE);
        retryButton.setFocusable(false); // Prevents the button from stealing focus from the panel
        retryButton.addActionListener(e -> restartGame()); // Add action listener for retry

        // Use GridBagLayout for centering the button
        this.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 1; // Position the button in a new row for separation
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(40, 0, 10, 0); // Add top padding for spacing
        this.add(retryButton, gbc);
        retryButton.setVisible(false); // Hide button initially

        initializeGame();
        startGame();
    }

    /**
     * Initializes game state variables.
     */
    private void initializeGame() {
        board = new int[BOARD_ROWS][BOARD_COLS];
        pelletsRemaining = 0;
        for (int r = 0; r < BOARD_ROWS; r++) {
            for (int c = 0; c < BOARD_COLS; c++) {
                board[r][c] = initialBoard[r][c];
                if (board[r][c] == 2 || board[r][c] == 3) {
                    pelletsRemaining++;
                }
            }
        }

        pacMan = new PacMan(PACMAN_START.x, PACMAN_START.y, 3); // 3 lives
        ghosts = new ArrayList<>();
        // Only add two ghosts
        ghosts.add(new Ghost(GHOST_START_1.x, GHOST_START_1.y, Color.RED));
        ghosts.add(new Ghost(GHOST_START_2.x, GHOST_START_2.y, Color.CYAN));
        // Removed adding ghosts for GHOST_START_3 and GHOST_START_4

        score = 0;
        running = false;
        gameOver = false;
        gameWon = false;
    }

    /**
     * Starts the game timer.
     */
    public void startGame() {
        running = true;
        if (timer != null) {
            timer.stop();
        }
        timer = new Timer(DELAY, this);
        timer.start();
        retryButton.setVisible(false);
        this.requestFocusInWindow(); // Ensure panel has focus for key events
    }

    /**
     * Restarts the game by re-initializing and starting.
     */
    private void restartGame() {
        initializeGame(); // Reset all game variables
        startGame();
    }

    /**
     * Overrides paintComponent to draw all game elements.
     * @param g The Graphics object.
     */
    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        draw(g);
    }

    /**
     * Custom draw method to render game objects.
     * @param g The Graphics object.
     */
    public void draw(Graphics g) {
        // Draw maze
        for (int r = 0; r < BOARD_ROWS; r++) {
            for (int c = 0; c < BOARD_COLS; c++) {
                int x = c * TILE_SIZE;
                int y = r * TILE_SIZE;

                // If it's a path, pellet, or power pellet cell, draw it
                if (board[r][c] == 0 || board[r][c] == 2 || board[r][c] == 3) {
                    // Draw a slightly darker border first for the path
                    g.setColor(new Color(20, 20, 20)); // Dark grey for border
                    g.fillRect(x, y, TILE_SIZE, TILE_SIZE);

                    // Then draw the inner part of the path in black
                    g.setColor(Color.BLACK);
                    g.fillRect(x + 1, y + 1, TILE_SIZE - 2, TILE_SIZE - 2); // Inset by 1 pixel for border
                }
                // Walls (board[r][c] == 1) will automatically be the panel's background color (BLUE)
                // No need to draw them explicitly here.

                if (board[r][c] == 2) { // Pellet
                    g.setColor(Color.WHITE);
                    g.fillOval(x + TILE_SIZE / 3, y + TILE_SIZE / 3, TILE_SIZE / 3, TILE_SIZE / 3);
                } else if (board[r][c] == 3) { // Power Pellet
                    g.setColor(Color.PINK);
                    g.fillOval(x + TILE_SIZE / 4, y + TILE_SIZE / 4, TILE_SIZE / 2, TILE_SIZE / 2);
                }
            }
        }

        // Draw Pac-Man
        g.setColor(Color.YELLOW);
        // Calculate the start angle for Pac-Man's mouth direction
        int startAngle = 0;
        if (pacMan.dx == 1) { // Right
            startAngle = (int) pacMan.mouthAngle;
        } else if (pacMan.dx == -1) { // Left
            startAngle = 180 + (int) pacMan.mouthAngle;
        } else if (pacMan.dy == -1) { // Up
            startAngle = 90 + (int) pacMan.mouthAngle;
        } else if (pacMan.dy == 1) { // Down
            startAngle = 270 + (int) pacMan.mouthAngle;
        }
        
        // Draw Pac-Man as an arc for mouth animation
        // The angle for the arc drawing goes counter-clockwise from 3 o'clock.
        // We subtract mouthAngle * 2 from 360 to get the sweep angle for the open mouth.
        g.fillArc(pacMan.x * TILE_SIZE, pacMan.y * TILE_SIZE,
                TILE_SIZE, TILE_SIZE,
                startAngle, 360 - (int) pacMan.mouthAngle * 2);


        // Draw Ghosts
        for (Ghost ghost : ghosts) {
            if (ghost.frightened) {
                g.setColor(Color.LIGHT_GRAY); // Frightened ghost color
                // Draw eyes for frightened ghosts (black eyes)
                g.setColor(Color.BLACK);
                g.fillOval(ghost.x * TILE_SIZE + TILE_SIZE/4, ghost.y * TILE_SIZE + TILE_SIZE/4, TILE_SIZE/5, TILE_SIZE/5);
                g.fillOval(ghost.x * TILE_SIZE + TILE_SIZE*2/4, ghost.y * TILE_SIZE + TILE_SIZE/4, TILE_SIZE/5, TILE_SIZE/5);
            } else {
                g.setColor(ghost.color);
            }
            g.fillOval(ghost.x * TILE_SIZE, ghost.y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            // Draw eyes for regular ghosts (white eyes with black pupils)
            g.setColor(Color.WHITE);
            g.fillOval(ghost.x * TILE_SIZE + TILE_SIZE/4, ghost.y * TILE_SIZE + TILE_SIZE/4, TILE_SIZE/4, TILE_SIZE/4); // Left eye
            g.fillOval(ghost.x * TILE_SIZE + TILE_SIZE*2/4 + 5, ghost.y * TILE_SIZE + TILE_SIZE/4, TILE_SIZE/4, TILE_SIZE/4); // Right eye
            g.setColor(Color.BLACK);
            g.fillOval(ghost.x * TILE_SIZE + TILE_SIZE/4 + 3, ghost.y * TILE_SIZE + TILE_SIZE/4 + 3, TILE_SIZE/8, TILE_SIZE/8); // Left pupil
            g.fillOval(ghost.x * TILE_SIZE + TILE_SIZE*2/4 + 8, ghost.y * TILE_SIZE + TILE_SIZE/4 + 3, TILE_SIZE/8, TILE_SIZE/8); // Right pupil
        }


        // Draw Score and Lives
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 20));
        g.drawString("Score: " + score, 10, SCREEN_HEIGHT - 30);
        g.drawString("Lives: " + pacMan.lives, SCREEN_WIDTH - 100, SCREEN_HEIGHT - 30);

        if (!running) {
            if (gameOver) {
                drawGameOver(g);
            } else if (gameWon) {
                drawGameWon(g);
            }
        }
    }

    /**
     * Draws the "Game Over" screen.
     * @param g The Graphics object.
     */
    private void drawGameOver(Graphics g) {
        g.setColor(Color.RED);
        g.setFont(new Font("Ink Free", Font.BOLD, 75));
        FontMetrics metrics1 = getFontMetrics(g.getFont());
        // Position "Game Over" text
        int textX = (SCREEN_WIDTH - metrics1.stringWidth("Game Over")) / 2;
        int textY = SCREEN_HEIGHT / 2 - 20 - (metrics1.getHeight() / 2); // Center vertically, adjusted for button

        g.drawString("Game Over", textX, textY);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Ink Free", Font.BOLD, 40));
        FontMetrics metrics2 = getFontMetrics(g.getFont());
        // Position score text
        textX = (SCREEN_WIDTH - metrics2.stringWidth("Final Score: " + score)) / 2;
        textY = textY - 50; // Above "Game Over"

        g.drawString("Final Score: " + score, textX, textY);
    }

    /**
     * Draws the "You Won!" screen.
     * @param g The Graphics object.
     */
    private void drawGameWon(Graphics g) {
        g.setColor(Color.GREEN);
        g.setFont(new Font("Ink Free", Font.BOLD, 75));
        FontMetrics metrics1 = getFontMetrics(g.getFont());
        int textX = (SCREEN_WIDTH - metrics1.stringWidth("You Won!")) / 2;
        int textY = SCREEN_HEIGHT / 2 - 20 - (metrics1.getHeight() / 2);

        g.drawString("You Won!", textX, textY);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Ink Free", Font.BOLD, 40));
        FontMetrics metrics2 = getFontMetrics(g.getFont());
        textX = (SCREEN_WIDTH - metrics2.stringWidth("Final Score: " + score)) / 2;
        textY = textY - 50;

        g.drawString("Final Score: " + score, textX, textY);
    }

    /**
     * Main game loop update logic.
     * @param e ActionEvent from the timer.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (running) {
            updateGame();
            checkGameStatus();
        }
        repaint();
    }

    /**
     * Updates game state: movement, collisions, etc.
     */
    private void updateGame() {
        // --- Pac-Man Movement ---
        int nextPacManX = pacMan.x + pacMan.dx;
        int nextPacManY = pacMan.y + pacMan.dy;

        // Check for wall collision before moving
        if (isValidMove(nextPacManX, nextPacManY)) {
            pacMan.move(); // Move Pac-Man if the next tile is not a wall
        } else {
            // Stop Pac-Man if he hits a wall
            pacMan.dx = 0;
            pacMan.dy = 0;
        }

        // Handle pellet/power pellet consumption
        if (board[pacMan.y][pacMan.x] == 2) {
            board[pacMan.y][pacMan.x] = 0; // Eat pellet
            score += 10;
            pelletsRemaining--;
        } else if (board[pacMan.y][pacMan.x] == 3) {
            board[pacMan.y][pacMan.x] = 0; // Eat power pellet
            score += 50;
            pelletsRemaining--;
            // Frighten ghosts
            for (Ghost ghost : ghosts) {
                ghost.frightened = true;
                ghost.frightenedTimer = System.currentTimeMillis() + POWER_PELLET_FRIGHTEN_TIME;
            }
        }

        // --- Ghost Movement ---
        for (Ghost ghost : ghosts) {
            // Check if frightened state has expired
            if (ghost.frightened && System.currentTimeMillis() > ghost.frightenedTimer) {
                ghost.frightened = false;
            }

            // Independent Ghost AI: Randomly choose a valid direction
            ArrayList<Point> possibleMoves = new ArrayList<>();
            // Check Up
            if (isValidMove(ghost.x, ghost.y - 1)) possibleMoves.add(new Point(0, -1));
            // Check Down
            if (isValidMove(ghost.x, ghost.y + 1)) possibleMoves.add(new Point(0, 1));
            // Check Left
            if (isValidMove(ghost.x - 1, ghost.y)) possibleMoves.add(new Point(-1, 0));
            // Check Right
            if (isValidMove(ghost.x + 1, ghost.y)) possibleMoves.add(new Point(1, 0));

            if (!possibleMoves.isEmpty()) {
                Point chosenDirection = possibleMoves.get(random.nextInt(possibleMoves.size()));
                ghost.dx = chosenDirection.x;
                ghost.dy = chosenDirection.y;
            } else {
                // If no valid moves (e.g., stuck in a corner), stop
                ghost.dx = 0;
                ghost.dy = 0;
            }

            int nextGhostX = ghost.x + ghost.dx;
            int nextGhostY = ghost.y + ghost.dy;

            // Ensure ghosts don't move into walls.
            // Simplified: Ghosts can move into any non-wall tile.
            if (board[nextGhostY][nextGhostX] != 1) {
                ghost.x = nextGhostX;
                ghost.y = nextGhostY;
            } else {
                // If hit a wall, clear direction to force re-evaluation next time
                ghost.dx = 0;
                ghost.dy = 0;
            }
        }

        // --- Collision Detection (Pac-Man vs Ghosts) ---
        for (Ghost ghost : ghosts) {
            if (pacMan.x == ghost.x && pacMan.y == ghost.y) {
                if (ghost.frightened) {
                    score += 200; // Score for eating frightened ghost
                    ghost.reset(GHOST_START_1.x, GHOST_START_1.y); // Send ghost back to starting point
                } else {
                    // Pac-Man loses a life
                    pacMan.lives--;
                    if (pacMan.lives <= 0) {
                        gameOver = true;
                        running = false;
                    } else {
                        // Reset Pac-Man and ghosts to starting positions
                        pacMan.reset(PACMAN_START.x, PACMAN_START.y);
                        ghosts.forEach(g -> g.reset(GHOST_START_1.x, GHOST_START_1.y)); // Reset all ghosts
                    }
                }
            }
        }
    }

    /**
     * Checks if a move to (x, y) is valid (not a wall).
     */
    private boolean isValidMove(int x, int y) {
        if (x < 0 || x >= BOARD_COLS || y < 0 || y >= BOARD_ROWS) {
            return false; // Out of bounds
        }
        // Ghosts can move through ghost house entrance (empty space), but Pac-Man cannot
        // Simplified: Pac-Man can't go into walls. Ghosts can't go into walls unless it's their "home"
        return board[y][x] != 1;
    }

    /**
     * Checks for win/loss conditions.
     */
    private void checkGameStatus() {
        if (pelletsRemaining == 0) {
            gameWon = true;
            running = false;
        }

        if (!running) {
            timer.stop();
            retryButton.setVisible(true);
        }
    }

    /**
     * Inner class to handle keyboard input.
     */
    public class MyKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            if (!running && (gameOver || gameWon)) {
                // If game is over/won, only allow retry button to be active, not movement
                return;
            }
            int newDx = pacMan.dx;
            int newDy = pacMan.dy;

            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT:
                    newDx = -1; newDy = 0;
                    break;
                case KeyEvent.VK_RIGHT:
                    newDx = 1; newDy = 0;
                    break;
                case KeyEvent.VK_UP:
                    newDx = 0; newDy = -1;
                    break;
                case KeyEvent.VK_DOWN:
                    newDx = 0; newDy = 1;
                    break;
            }

            // Check if the new direction is valid (not immediately into a wall)
            int checkX = pacMan.x + newDx;
            int checkY = pacMan.y + newDy;

            if (isValidMove(checkX, checkY)) {
                pacMan.dx = newDx;
                pacMan.dy = newDy;
            }
        }
    }
}
