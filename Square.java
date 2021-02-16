import java.util.ArrayList;

public class Square
{
  public byte row;
  public byte col;
  Square(int irow, int icol)
  {
    row = (byte)irow;
    col = (byte)icol;
  }

  public boolean equals(Square s)
  {
    return this.row == s.row && this.col == s.col;
  }

  public void setAs(Square s)
  {
    this.row = s.row;
    this.col = s.col;
  }

  public void setAs(int row, int col)
  {
    this.row = (byte)row;
    this.col = (byte)col;
  }

  public boolean isInSquares(ArrayList<Square> list)
  {
    int size = list.size();
    for (int i = 0; i < size; i++)
    {
      if (list.get(i).equals(this))
      {
        return true;
      }
    }
    return false;
  }
}