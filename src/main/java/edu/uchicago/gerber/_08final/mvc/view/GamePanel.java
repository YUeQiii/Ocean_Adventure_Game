package edu.uchicago.gerber._08final.mvc.view;

import edu.uchicago.gerber._08final.mvc.controller.CommandCenter_new;
import edu.uchicago.gerber._08final.mvc.controller.Game_new;
import edu.uchicago.gerber._08final.mvc.controller.Utils;
import edu.uchicago.gerber._08final.mvc.model.Movable;
import edu.uchicago.gerber._08final.mvc.model.PolarPoint;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;


public class GamePanel extends Panel {

    // ==============================================================
    // FIELDS
    // ==============================================================
    private final Font fontNormal = new Font("SansSerif", Font.BOLD, 12);
    private final Font fontBig = new Font("SansSerif", Font.BOLD + Font.ITALIC, 36);
    private FontMetrics fontMetrics;
    private int fontWidth;
    private int fontHeight;

    //used to draw number of ships remaining
    private final Point[] pntShipsRemaining;

    //used for double-buffering
    private Image imgOff;
    private Graphics grpOff;


    // ==============================================================
    // CONSTRUCTOR
    // ==============================================================

    public GamePanel(Dimension dim) {

        GameFrame gameFrame = new GameFrame();

        gameFrame.getContentPane().add(this);

        List<Point> listPoints = new ArrayList<>();

        listPoints.add(new Point(0, 3));
        listPoints.add(new Point(-2, 2));
        listPoints.add(new Point(-3, 0));
        listPoints.add(new Point(0, -2));
        listPoints.add(new Point(-1, -3));
        listPoints.add(new Point(1, -3));
        listPoints.add(new Point(0, -2));
        listPoints.add(new Point(3, 0));
        listPoints.add(new Point(2, 2));
        listPoints.add(new Point(0, 3));

        pntShipsRemaining = listPoints.toArray(new Point[0]);

        gameFrame.pack();
        initFontInfo();
        gameFrame.setSize(dim);
        //change the name of the game-frame to your game name
        gameFrame.setTitle("Adventures of the Ocean");
        gameFrame.setResizable(false);
        gameFrame.setVisible(true);
        setFocusable(true);
    }


    // ==============================================================
    // METHODS
    // ==============================================================

    private void drawFalconStatus(final Graphics graphics){

        graphics.setColor(Color.white);
        graphics.setFont(fontNormal);

        //draw score always
        graphics.drawString("Score :  " + CommandCenter_new.getInstance().getScore(), fontWidth, fontHeight);

        //draw the level upper-left corner always
        String levelText = "Level: " + CommandCenter_new.getInstance().getLevel();
        graphics.drawString(levelText, 20, 30); //upper-left corner

        //build the status string array with possible messages in middle of screen
        List<String> statusArray = new ArrayList<>();
        if (CommandCenter_new.getInstance().getFish().getShowLevel() > 0) statusArray.add(levelText);
        if (CommandCenter_new.getInstance().getFish().isMaxSpeedAttained()) statusArray.add("WARNING - SLOW DOWN");
        if (CommandCenter_new.getInstance().getFish().getNukeMeter() > 0) statusArray.add("PRESS N for NUKE");
        if (CommandCenter_new.getInstance().getFish().getMultiDirectionFireMeter()>0) statusArray.add("YOU CAN HAVE MULTI_DIRECTIONAL FIRE");

        //draw the statusArray strings to middle of screen
        if (statusArray.size() > 0)
            displayTextOnScreen(graphics, statusArray.toArray(new String[0]));
    }

    //this is used for development, you can remove it from your final game
    private void drawNumFrame(Graphics g) {
        g.setColor(Color.white);
        g.setFont(fontNormal);
        g.drawString("FRAME :  " + CommandCenter_new.getInstance().getFrame(), fontWidth,
                Game_new.DIM.height  - (fontHeight + 22));

    }


    private void drawMeters(Graphics g){

        //will be a number between 0-100 inclusive
        int shieldValue =   CommandCenter_new.getInstance().getFish().getShield() / 2;
        int nukeValue = CommandCenter_new.getInstance().getFish().getNukeMeter() /2;
        int multiValue = CommandCenter_new.getInstance().getFish().getMultiDirectionFireMeter()/2;

        drawOneMeter(g, Color.PINK, 2, shieldValue);
        drawOneMeter(g, Color.YELLOW, 3, nukeValue);
        drawOneMeter(g, Color.GREEN, 4, multiValue);


    }

    private void drawOneMeter(Graphics g, Color color, int offSet, int percent) {

        int xVal = Game_new.DIM.width - (100 + 100 * offSet);
        int yVal = Game_new.DIM.height - 45;

        //draw meter
        g.setColor(color);
        g.fillRect(xVal, yVal, percent, 10);

        //draw gray box
        g.setColor(Color.DARK_GRAY);
        g.drawRect(xVal, yVal, 100, 10);
    }

    @Override
    public void update(Graphics g) {

        // The following "off" vars are used for the off-screen double-buffered image.
        imgOff = createImage(Game_new.DIM.width, Game_new.DIM.height);
        //get its graphics context
        grpOff = imgOff.getGraphics();

        //Fill the off-screen image background with black.
        Color color = new Color(50,160,170);
        grpOff.setColor(color);
        grpOff.fillRect(0, 0, Game_new.DIM.width, Game_new.DIM.height);

        //this is used for development, you may remove drawNumFrame() in your final game.
        drawNumFrame(grpOff);

        if (CommandCenter_new.getInstance().isGameOver()) {
            displayTextOnScreen(grpOff,
                    "GAME OVER",
                    "use the arrow keys to turn,thrust and doge",
                    "use the space bar to fire",
                    "'S' to Start",
                    "'P' to Pause",
                    "'Q' to Quit",
                    "'M' to toggle music"

            );
        } else if (CommandCenter_new.getInstance().isPaused()) {

            displayTextOnScreen(grpOff, "Game Paused");

        }

        //playing and not paused!
        else {


            moveDrawMovables(grpOff,
                    CommandCenter_new.getInstance().getMovDebris(),
                    CommandCenter_new.getInstance().getMovFloaters(),
                    CommandCenter_new.getInstance().getMovFoes(),
                    CommandCenter_new.getInstance().getMovFriends());


            drawNumberShipsRemaining(grpOff);
            drawMeters(grpOff);
            drawFalconStatus(grpOff);


        }

        //after drawing all the movables or text on the offscreen-image, copy it in one fell-swoop to graphics context
        // of the game panel, and show it for ~40ms. If you attempt to draw sprites directly on the gamePanel, e.g.
        // without the use of a double-buffered off-screen image, you will see flickering.
        g.drawImage(imgOff, 0, 0, this);
    }


    //this method causes all sprites to move and draw themselves
    @SafeVarargs
    private final void moveDrawMovables(final Graphics g, List<Movable>... teams) {

        BiConsumer<Movable, Graphics> moveDraw = (mov, grp) -> {
            mov.move();
            mov.draw(grp);
        };


        Arrays.stream(teams) //Stream<List<Movable>>
                //we use flatMap to flatten the teams (List<Movable>[]) passed-in above into a single stream of Movables
                .flatMap(Collection::stream) //Stream<Movable>
                .forEach(m -> moveDraw.accept(m, g));


    }




    // Draw the number of falcons remaining on the bottom-right of the screen.
    private void drawNumberShipsRemaining(Graphics g) {
        int numFalcons = CommandCenter_new.getInstance().getNumFishes();
        while (numFalcons > 0) {
            drawOneShip(g, numFalcons--);
        }
    }


    private void drawOneShip(Graphics g, int offSet) {

        g.setColor(Color.ORANGE);

        //rotate the ship 90 degrees
        double degrees90 = 90.0;
        int radius = 15;
        int xVal = Game_new.DIM.width - (27 * offSet);
        int yVal = Game_new.DIM.height - 45;

        //the reason we convert to polar-points is that it's much easier to rotate polar-points.
        List<PolarPoint> polars = Utils.cartesianToPolar(pntShipsRemaining);

        Function<PolarPoint, PolarPoint> rotatePolarBy90 =
                pp -> new PolarPoint(
                        pp.getR(),
                        pp.getTheta() + Math.toRadians(degrees90) //rotated Theta
                );

        Function<PolarPoint, Point> polarToCartesian =
                pp -> new Point(
                        (int)  (pp.getR() * radius * Math.sin(pp.getTheta())),
                        (int)  (pp.getR() * radius * Math.cos(pp.getTheta())));

        Function<Point, Point> adjustForLocation =
                pnt -> new Point(
                        pnt.x + xVal,
                        pnt.y + yVal);


        g.drawPolygon(

                polars.stream()
                        .map(rotatePolarBy90)
                        .map(polarToCartesian)
                        .map(adjustForLocation)
                        .map(pnt -> pnt.x)
                        .mapToInt(Integer::intValue)
                        .toArray(),

                polars.stream()
                        .map(rotatePolarBy90)
                        .map(polarToCartesian)
                        .map(adjustForLocation)
                        .map(pnt -> pnt.y)
                        .mapToInt(Integer::intValue)
                        .toArray(),

                polars.size());


    }

    private void initFontInfo() {
        Graphics g = getGraphics();            // get the graphics context for the panel
        g.setFont(fontNormal);                        // take care of some simple font stuff
        fontMetrics = g.getFontMetrics();
        fontWidth = fontMetrics.getMaxAdvance();
        fontHeight = fontMetrics.getHeight();
        g.setFont(fontBig);                    // set font info
    }


    // This method draws some text to the middle of the screen
    private void displayTextOnScreen(final Graphics graphics, String... lines) {

        //AtomicInteger is safe to pass into a stream
        final AtomicInteger spacer = new AtomicInteger(0);
        Arrays.stream(lines)
                .forEach(str ->
                            graphics.drawString(str, (Game_new.DIM.width - fontMetrics.stringWidth(str)) / 2,
                                    Game_new.DIM.height / 4 + fontHeight + spacer.getAndAdd(40))

                );


    }


}
