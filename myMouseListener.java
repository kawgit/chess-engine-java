import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Graphics;

public class myMouseListener extends MouseAdapter
{
  Graphics g;
  GameState gs;
  ChessApp ap;
  
  myMouseListener(Graphics g, GameState gs, ChessApp ap)
  {
    this.g = g;
    this.gs = gs;
    this.ap = ap;
  }

  @Override
  public void mouseClicked(MouseEvent e) 
  { 
    // TODO Auto-generated method stub

  }

  @Override
  public void mousePressed(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mouseReleased(MouseEvent e) {
    int y = e.getY()-32;
    int x = e.getX()-8;
    Square hit = new Square(ap.getRow(y), ap.getCol(x));
    System.out.println("Released");
    if (hit.row >= 0 && hit.row <= 7 && hit.col >= 0 && hit.col <=7)
    {
      if (ap.selected != null) //is already selected?
      {
        if (hit.isInSquares(ap.highlighted)) //make move
        {
          System.out.println("make");
          gs.makeMove(gs.getMoveFromValid(new Move(ap.mirRow(ap.selected.row), ap.mirCol(ap.selected.col), ap.mirRow(hit.row), ap.mirCol(hit.col), gs.board)));
          gs.currentValidMoves = gs.getValidMoves();
          ap.resetBoard(g);
        }
        else if (hit.equals(ap.selected)) //deselect
        {
          System.out.println("deselect");
          ap.selected = null;
          ap.resetBoard(g);
        }
        else //select
        {
          System.out.println("select");
          ap.selectSquare(g, hit.row, hit.col);
        }
      }
      else //select
      {
        System.out.println("select");
        ap.selectSquare(g, hit.row, hit.col);
      }
    }
    else if (x >= ap.undoX && x <= ap.undoX + ap.squareIncrement && y >= ap.undoY && y <= ap.undoY + ap.squareIncrement && gs.moveLog.size() > 0)
    {
      System.out.println("undo");
      gs.undoMove();
      if (gs.aiMode != 0)
      {
        gs.undoMove();
      }
      gs.currentValidMoves = gs.getValidMoves();
      ap.resetBoard(g);
    }
    
    if (gs.turn == gs.aiMode)
    {
      System.out.println("deselect");
      ap.selected = null;
      ap.resetBoard(g);
      gs.makeMove(gs.getBestMove());
      gs.currentValidMoves = gs.getValidMoves();
      ap.resetBoard(g);
    }
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    // TODO Auto-generated method stub

  }

  @Override
  public void mouseExited(MouseEvent e) {
    // TODO Auto-generated method stub

  }
}
