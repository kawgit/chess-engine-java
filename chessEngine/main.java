import java.lang.reflect.Array;
import java.util.Random;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Scanner;
import java.util.ArrayList;

/*
Piece list for reference (need to be able to differenciate between the following listed possible states of squares): 
{
pawn
knight
bishop
rook
queen
knight
} x2 
empty
BIT REPRESENTATION: USING BOOLEAN ARR INSTEAD 
FOR ALL: index = row*8+col
array with size 64, 1 byte per square. 
key: (negative for black pieces)
0 empty
1 wpawn
2 wknight
3 wbishop
4 wrook
5 wqueen
6 wking
*/
class Main {
  public static void main(String[] args) {
    var gs = new gameState();
    boolean running = true;

    gs.printLayout();

    Scanner scanner = new Scanner(System.in);
    while (running)//player vs player or player vs ai format
    {
      gs.printBoard();
      System.out.println("enter move: \n");
      String input = scanner.nextLine();
      if (isNumeric(input) && gs.isInValid(new Move(input)))
      {
        gs.makeMove(new Move(input));
      }
      else if (input.equals("e"))
      {
        running = false;
      }
      else 
      {
        System.out.println("unknown command <"+input+">");
      }
      
    }
    if (!running)
    {
      scanner.close();        
    }
    
  }
  public static boolean isNumeric(String str)
  {
    try
    {
      Integer.parseInt(str);
      return true;
    }
    catch (Exception e)
    {
      return false;
    }
  }
}

class gameState {
  public byte board[] = new byte[]
  {
     4, 2, 3, 5, 6, 3, 2, 4,
     1, 1, 1, 1, 1, 1, 1, 1,
     0, 0, 0, 0, 0, 0, 0, 0,
     0, 0, 0, 0, 0, 0, 0, 0,
     0, 0, 0, 0, 0, 0, 0, 0,
     0, 0, 0, 0, 0, 0, 0, 0,
    -1,-1,-1,-1,-1,-1,-1,-1,
    -4,-2,-3,-5,-6,-3,-2,-4
  };
  public int turn = 1; // or -1
  public HashMap<Integer, String> boardPrintKey = new HashMap<Integer, String>();
  public ArrayList<Square> knightMoves = new ArrayList<Square>();
  public ArrayList<Square> bishopDirections = new ArrayList<Square>();
  public ArrayList<Square> rookDirections = new ArrayList<Square>();
  public ArrayList<Square> queenDirections = new ArrayList<Square>();
  public ArrayList<Square> kingMoves = new ArrayList<Square>();
  gameState()
  {
    boardPrintKey.put(0,"--");
    boardPrintKey.put(1,"wP");
    boardPrintKey.put(2,"wN");
    boardPrintKey.put(3,"wB");
    boardPrintKey.put(4,"wR");
    boardPrintKey.put(5,"wQ");
    boardPrintKey.put(6,"wK");
    boardPrintKey.put(-1,"bP");
    boardPrintKey.put(-2,"bN");
    boardPrintKey.put(-3,"bB");
    boardPrintKey.put(-4,"bR");
    boardPrintKey.put(-5,"bQ");
    boardPrintKey.put(-6,"bK");
    knightMoves.add(new Square(1,2));
    knightMoves.add(new Square(1,-2));
    knightMoves.add(new Square(2,1));
    knightMoves.add(new Square(2,-1));
    knightMoves.add(new Square(-1,2));
    knightMoves.add(new Square(-1,-2));
    knightMoves.add(new Square(-2,1));
    knightMoves.add(new Square(-2,-1));

    bishopDirections.add(new Square(1,-1));
    bishopDirections.add(new Square(1,1));
    bishopDirections.add(new Square(-1,-1));
    bishopDirections.add(new Square(-1,1));

    rookDirections.add(new Square(1,0));
    rookDirections.add(new Square(0,1));
    rookDirections.add(new Square(-1,0));
    rookDirections.add(new Square(0,-1));

    queenDirections.addAll(bishopDirections);
    queenDirections.addAll(rookDirections);
  }

  public void printBoard()
  {
    String str = "";
    for (int row = 7; row >= 0; row--)
    {
      str += String.valueOf(row) + " ";
      for (int col = 0; col < 8; col++)
      {
        str += boardPrintKey.get((int)board[row*8+col]) + " ";
      }
      str += "\n";
    }
    System.out.println(str + "  0  1  2  3  4  5  6  7");
  }

  public void printLayout()
  {
    String str = "";
    for (int row = 7; row >= 0; row--)
    {
      str += String.valueOf(row) + " ";
      for (int col = 0; col < 8; col++)
      {
        str += String.valueOf(row*8+col) + " ";
      }
      str += "\n";
    }
    System.out.println(str + "  0  1  2  3  4  5  6  7");
  }

  public void makeMove(Move move)
  {
    board[move.erow*8+move.ecol] = board[move.srow*8+move.scol];
    board[move.srow*8+move.scol] = 0;
    if (move.isCastle)
    {
      if (move.ecol == 6)
      {
        board[move.srow*8+5] = board[move.srow*8+7];
        board[move.srow*8+7] = 0;
      }
      else
      {
        board[move.srow*8+2] = board[move.srow*8];
        board[move.srow*8] = 0;
      }
    }
    else if (move.isEnPassent)
    {
      board[move.srow*8+move.ecol] = 0;
    }
    turn = turn == 1? -1 : 1;
  }

  public ArrayList<Move> getValidMoves()
  {
    var validMoves = new ArrayList<Move>();
    for (int row = 0; row < 8; row++)
    {
      for (int col = 0; col < 8; col++)
      {
        int peice = board[row*8+col];
        if (peice != 0)
        {
          if (peice/Math.abs(peice) == turn)
          {
            if (Math.abs(peice) == 1)
            {
              validMoves.addAll(getPawnMoves(row, col));
            }
            else if (Math.abs(peice) == 2)
            {
              validMoves.addAll(getKnightMoves(row, col));
            }
            else if (Math.abs(peice) == 3)
            {
              validMoves.addAll(getDirectionalMoves(row, col, bishopDirections));
            }
            else if (Math.abs(peice) == 4)
            {
              validMoves.addAll(getDirectionalMoves(row, col, rookDirections));
            }
            else if (Math.abs(peice) == 5)
            {
              validMoves.addAll(getDirectionalMoves(row, col, queenDirections));
            }
            else if (Math.abs(peice) == 6)
            {
              validMoves.addAll(getKingMoves(row, col));
            }
          }
        }
      }
    }
    return validMoves;
  }

  void getChecks()
  {

  }

  public ArrayList<Move> getPawnMoves(int row, int col)
  {
    var validMoves = new ArrayList<Move>();
    if (board[(row+turn)*8+col] == 0) //1 forward
    {
      validMoves.add(new Move(row, col, row+turn, col));
      if ((row == 1 && turn == 1)||(row == 6 && turn == -1))
      {
        validMoves.add(new Move(row, col, row+(turn*2), col));
      }
    }
    if (board[(row+turn)*8+col+1] != 0 && board[(row+turn)*8+col+1]/Math.abs(board[(row+turn)*8+col+1]) == -turn) //forward right capture
    {
      validMoves.add(new Move(row, col, row+turn, col+1));
    }
    if (board[(row+turn)*8+col-1] != 0 && board[(row+turn)*8+col-1]/Math.abs(board[(row+turn)*8+col-1]) == -turn)//forward left capture
    {
      validMoves.add(new Move(row, col, row+turn, col-1));
    }
    return validMoves;
  }
  
  public ArrayList<Move> getKnightMoves(int row, int col)
  {
    var validMoves = new ArrayList<Move>();
    for (int i = 0; i < 8; i++)
    {
      Square square = knightMoves.get(i);
      int drow = square.row;
      int dcol = square.col;
      if (row+drow >= 0 && row+drow <= 7 && col+dcol >= 0 && col+dcol <= 7)
      {
        int epeice = board[(row+drow)*8+col+dcol];
        if (epeice == 0 || epeice/Math.abs(epeice) == -turn)
        {
          validMoves.add(new Move(row, col, row+drow, col+dcol));
        }        
      } 
    }
    return validMoves;
  }
  public ArrayList<Move> getKingMoves(int row, int col)
  {
    var validMoves = new ArrayList<Move>();
    for (int i = 0; i < 8; i++)//theta, aka direction that we are exploring (just saying theta bc im cool)
    {
      int drow = queenDirections.get(i).row;
      int dcol = queenDirections.get(i).col;
      if (row+drow >= 0 && row+drow <= 7 && col+dcol >= 0 && col+dcol <= 7)
      {
        int epeice = board[(row+drow)*8+col+dcol];
        if (epeice == 0 || epeice/Math.abs(epeice) == -turn)
        {
          validMoves.add(new Move(row, col, row+drow, col+dcol));
        }
      }
    }
    return validMoves;
  }
  public ArrayList<Move> getDirectionalMoves(int row, int col, ArrayList<Square> directions)
  {
    var validMoves = new ArrayList<Move>();
    for (int i = 0; i < directions.size(); i++)//theta, aka direction that we are exploring (just saying theta bc im cool)
    {
      for (int m = 1; m < 8; m++)//m for magnitude (again precalc terminology hehe)
      {
        int drow = directions.get(i).row*m;
        int dcol = directions.get(i).col*m;
        if (row+drow >= 0 && row+drow <= 7 && col+dcol >= 0 && col+dcol <= 7)
        {
          int epeice = board[(row+drow)*8+col+dcol];
          if (epeice == 0 || epeice/Math.abs(epeice) == -turn)
          {
            validMoves.add(new Move(row, col, row+drow, col+dcol));
          }
          else
          {
            break;
          }
        }
        else
        {
          break;
        }
      }
    }
    return validMoves;
  }
  
  public boolean isInValid(Move move)
  {
    var validMoves = getValidMoves();
    int size = validMoves.size();
    for (int i = 0; i < size; i++)
    {
      System.out.println(validMoves.get(i).moveID);
      if (move.moveID.equals(validMoves.get(i).moveID))
      {
        return true;
      }
    }
    return false;
  }
}

class Move
{
  public int srow;
  public int scol;
  public int erow;
  public int ecol;
  public boolean isCastle;
  public boolean isEnPassent;
  public String moveID;
  Move(int startRow, int startCol, int endRow, int endCol)
  {
    srow = startRow;
    scol = startCol;
    erow = endRow;
    ecol = endCol;
    moveID = String.valueOf(startRow) + String.valueOf(startCol) + String.valueOf(endRow) + String.valueOf(endCol);
  }
  Move(int startRow, int startCol, int endRow, int endCol, boolean isSpecial)//CASTLE BOOLEAN = true, EN PASSANT BOOLEAN = false
  {
    srow = startRow;
    scol = startCol;
    erow = endRow;
    ecol = endCol;
    isCastle = isSpecial;
    isEnPassent = !isSpecial;
    moveID = String.valueOf(startRow) + String.valueOf(startCol) + String.valueOf(endRow) + String.valueOf(endCol);
  }
  Move(String MoveID)
  {
    srow = Integer.parseInt(MoveID.substring(0,1));
    scol = Integer.parseInt(MoveID.substring(1,2));
    erow = Integer.parseInt(MoveID.substring(2,3));
    ecol = Integer.parseInt(MoveID.substring(3,4));
    moveID = MoveID;
  }
}

class Square
{
  public int row;
  public int col;
  Square(int irow, int icol)
  {
    row = irow;
    col = icol;
  }
}