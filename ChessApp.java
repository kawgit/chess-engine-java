import java.awt.*;
import javax.swing.*;

import java.util.ArrayList;
import java.util.HashMap;

class ChessApp extends Panel{
  public static void main(String[] args) throws InterruptedException
  {
    new ChessApp();
  }

  public Color lightSquareColor = new Color(181, 181, 167);
  public Color darkSquareColor = new Color(86, 94, 76);
  public Color selectedColor = new Color(127, 127, 200, 127);
  public Color validMoveColor = new Color(0, 200, 0, 225);
  public ArrayList<Square> highlighted = new ArrayList<Square>();
  public boolean isFacingWhite = true;

  float pieceScale = .7f;
  int initialwidth = 2560;
  int initialheight = 1440;
  int height;
  int width;

  public int undoY;
  public int undoX;

  public Square selected;

  static Image undo;

  static Image wP;
  static Image wN;
  static Image wB;
  static Image wR;
  static Image wQ;
  static Image wK;

  static Image bP;
  static Image bN;
  static Image bB;
  static Image bR;
  static Image bQ;
  static Image bK;

  static HashMap<Integer, Image> intToImg;

  public int boardSize = Math.round(Math.min(initialwidth, initialheight) * (float) .9);
  public int squareIncrement = boardSize / 8;

  public JFrame frame;
  public ChessApp panel;

  public GameState gs;

  ChessApp(){
    gs = new GameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 1, 5, 30, 53253243);

    loadImages();

    frame = new JFrame("Kawgit's Chess Program");

    panel = this;
    panel.setSize(initialwidth, initialheight);
    panel.setBackground(Color.DARK_GRAY);

    frame.add(panel);
    frame.pack();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(initialwidth, initialheight);

    frame.setVisible(true);

    this.addMouseListener(new myMouseListener(panel.getGraphics(), gs, this));
  }

  public void paint(Graphics g) {
    System.out.println("@OVERIDE PAINT");
    width = frame.getWidth();
    height = frame.getHeight();
    boardSize = Math.round(Math.min(width, height) * (float) .9);
    squareIncrement = boardSize / 8;

    resetBoard(g);

    undoX = Math.round(width * .1F) - squareIncrement/2;
    undoY = (height - squareIncrement)/2;
    g.drawImage(undo, undoX, undoY, squareIncrement, squareIncrement, this);
  }

  public void paintPiece(Graphics g, int col, int row) {
    int index = (isFacingWhite ? 7-row : row) * 8 + (isFacingWhite ? col : 7-col);
    if (gs.board[index] != 0) {
      g.drawImage(intToImg.get((int) gs.board[index]),
          (col * squareIncrement) + (width / 2) - (boardSize / 2)
              + Math.round(squareIncrement * (1 - pieceScale) * .5f),
          (row * squareIncrement) + (height / 2) - (boardSize / 2)
              + Math.round(squareIncrement * (1 - pieceScale) * .5f),
          Math.round(squareIncrement * pieceScale), Math.round(squareIncrement * pieceScale), frame);
    }
  }

  public void loadImages() {
    Toolkit t = Toolkit.getDefaultToolkit();
    wP = t.getImage("wP.png");
    wN = t.getImage("wN.png");
    wB = t.getImage("wB.png");
    wR = t.getImage("wR.png");
    wQ = t.getImage("wQ.png");
    wK = t.getImage("wK.png");
    bP = t.getImage("bP.png");
    bN = t.getImage("bN.png");
    bB = t.getImage("bB.png");
    bR = t.getImage("bR.png");
    bQ = t.getImage("bQ.png");
    bK = t.getImage("bK.png");
    undo = t.getImage("undo.png");
    intToImg = new HashMap<Integer, Image>();
    intToImg.put(1, wP);
    intToImg.put(2, wN);
    intToImg.put(3, wB);
    intToImg.put(4, wR);
    intToImg.put(5, wQ);
    intToImg.put(6, wK);
    intToImg.put(7, bP);
    intToImg.put(8, bN);
    intToImg.put(9, bB);
    intToImg.put(10, bR);
    intToImg.put(11, bQ);
    intToImg.put(12, bK);
  }

  public int getRow(int y) //coord can be x OR y
  {
    return (y - (height - boardSize) / 2) / squareIncrement;
  }

  public int getCol(int x)
  {
    return (x - (width - boardSize) / 2) / squareIncrement;
  }

  public void paintSquare(Graphics g, int row, int col, Color c)
  {
    if (row >= 0 && row <= 7 && col >= 0 && col <= 7) 
    {
      g.setColor(c);
      g.fillRect((col * squareIncrement) + (width / 2) - (boardSize / 2),
      (row * squareIncrement) + (height / 2) - (boardSize / 2), squareIncrement, squareIncrement); 
    }
  }

  public void resetSquare(Graphics g, int row, int col)
  {
    paintSquare(g, row, col, (row + col) % 2 == 0 ? lightSquareColor : darkSquareColor);
    paintPiece(g, col, row);
  }

  public void selectSquare(Graphics g, int row, int col) //assumes is diffrent square than already selected
  {
    System.out.println("SELECT SQUARE FUCNTION");
    unhighlightPossibleMoves(g);
    if (selected != null)
    {
      resetSquare(g, selected.row, selected.col);
      selected.setAs(row, col);
    }
    else 
    {
      selected = new Square(row, col);
    }
    paintSquare(g, row, col, selectedColor);
    highlightPossibleMoves(g);
  }

  public void highlightPossibleMoves(Graphics g)
  {
    int size = gs.currentValidMoves.size();
    for (int i = 0; i < size; i++)
    {
      Move move = gs.currentValidMoves.get(i);
      if (mirRow(move.srow) == selected.row && mirCol(move.scol) == selected.col)
      {
        paintSquare(g, mirRow(move.erow), mirCol(move.ecol), validMoveColor);
        highlighted.add(new Square(mirRow(move.erow), mirCol(move.ecol)));
      }
    }
  }

  public void unhighlightPossibleMoves(Graphics g)
  {
    int size = highlighted.size();
    for (int i = 0; i < size; i++)
    {
      resetSquare(g, highlighted.get(i).row, highlighted.get(i).col);
    }
    highlighted.clear();
  }

  public void resetBoard(Graphics g)
  {
    for (int row = 0; row < 8; row++)
    {
      for (int col = 0; col < 8; col++)
      {
        resetSquare(g, row, 7-col);
      }
    }

    highlighted.clear();
    selected = null;
  }

  public int mirRow(int n) //check for mirror of row
  {
    return (isFacingWhite ? 7-n : n);
  }

  public int mirCol(int n) //check for mirror of col
  {
    return (isFacingWhite ? n : 7-n);
  }
}