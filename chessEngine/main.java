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


    var gs = new gameState("6k1/1pN3pn/b1np3p/p7/7N/1P4PP/P5BK/4r3 w - - 0 25", 1, 7);


    boolean running = true;

    Scanner scanner = new Scanner(System.in);
    while (running)//player vs player or player vs ai format
    {
      if (gs.turn == gs.aiMode)
      {
        gs.makeMove(gs.getBestMove());
      }
      gs.printBoard();
      System.out.println("enter move: \n");
      String input = scanner.nextLine();
      if (input.length() == 4)
      {
        Move move = gs.isInValid(input);
        if (move.moveID != "")
        {
          gs.makeMove(move);
        }        
      }
      else if (input.equals("u"))
      {
        gs.undoMove();
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
  public byte board[] = new byte[64];
  public int turn; //1 and -1
  public int aiMode; //0 = off, 1 = play for positive(white) (will be able to play for different colors in future, for now it plays as white only)
  public int startDepth;

  HashMap<Integer, String> boardPrintKey = new HashMap<Integer, String>();
  HashMap<String, Integer> fenKey = new HashMap<String, Integer>();

  ArrayList<Square> knightMoves = new ArrayList<Square>();
  ArrayList<Square> bishopDirections = new ArrayList<Square>();
  ArrayList<Square> rookDirections = new ArrayList<Square>();
  ArrayList<Square> queenDirections = new ArrayList<Square>();
  ArrayList<Square> kingMoves = new ArrayList<Square>();
  ArrayList<Square[]> pins = new ArrayList<Square[]>();
  ArrayList<Square[]> checks = new ArrayList<Square[]>();
  ArrayList<Move> moveLog = new ArrayList<Move>();
  ArrayList<CastleRights> castleRightsLog = new ArrayList<CastleRights>();
  ArrayList<Square> enPassantLog = new ArrayList<Square>();

  Square pKloc; //positive king location
  Square nKloc; //negative king location
  Square enPassant = new Square(0,0); //possible location of enpassant capture (erow, ecol)
  CastleRights castleRights = new CastleRights(false, false, false, false);


  gameState(String fen, int IaiMode, int Istartdepth)
  {
    aiMode = IaiMode;
    startDepth = Istartdepth;

    //constructed here because it is needed for crreation of board
    fenKey.put("p", -1);
    fenKey.put("n", -2);
    fenKey.put("b", -3);
    fenKey.put("r", -4);
    fenKey.put("q", -5);
    fenKey.put("k", -6);
    fenKey.put("P", 1);
    fenKey.put("N", 2);
    fenKey.put("B", 3);
    fenKey.put("R", 4);
    fenKey.put("Q", 5);
    fenKey.put("K", 6);

    //contruct lists and maps
    int square = 0;
    for (int i = 0; i < fen.length(); i++)
    {
      String c = fen.substring(i, i+1);
      if (square <= 63)
      {
        if (isNumeric(c))
        {
          square += Integer.valueOf(c);
        }
        else if (!c.equals("/"))
        {
          int a = fenKey.get(c);
          board[-square + square%8 + square%8 + 56] = (byte)a;
          square++;
        }        
      }
      else 
      {
        if (c.equals("w"))
        {
          turn = 1;
        }
        else if (c.equals("b"))
        {
          turn = -1;
        }
        else if (c.equals("K"))
        {
          castleRights.wKs = true;
        }
        else if (c.equals("k"))
        {
          castleRights.bKs = true;
        }
        else if (c.equals("Q"))
        {
          castleRights.wQs = true;
        }
        else if (c.equals("q"))
        {
          castleRights.bQs = true;
        }
      }
    }
    cgv();
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

  void cgv() //construct game variables
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

    bishopDirections.add(new Square(1,1));
    bishopDirections.add(new Square(1,-1));
    bishopDirections.add(new Square(-1,1));
    bishopDirections.add(new Square(-1,-1));

    rookDirections.add(new Square(1,0));
    rookDirections.add(new Square(0,1));
    rookDirections.add(new Square(-1,0));
    rookDirections.add(new Square(0,-1));

    queenDirections.addAll(bishopDirections);
    queenDirections.addAll(rookDirections);

    for (int row = 0; row < 8; row++)
    {
      for (int col = 0; col < 8; col++)
      {
        if (board[row*8+col] == 6)
        {
          pKloc = new Square(row, col);
        }
        else if (board[row*8+col] == -6)
        {
          nKloc = new Square(row, col);
        }
      }
    }
    castleRightsLog.add(new CastleRights(castleRights.wKs, castleRights.wQs, castleRights.bKs, castleRights.bQs));
    enPassantLog.add(new Square(enPassant.row, enPassant.col));
  }

  public void printBoard()
  {
    String str = "";
    for (int row = 7; row >= 0; row--)
    {
      str += String.valueOf(row+1) + " ";
      for (int col = 0; col < 8; col++)
      {
        str += boardPrintKey.get((int)board[row*8+col]) + " ";
      }
      str += "\n";
    }
    System.out.println(str + "  a  b  c  d  e  f  g  h");

    System.out.println(String.valueOf(enPassant.row)+String.valueOf(enPassant.col));
    System.out.println(String.valueOf(pKloc.row)+String.valueOf(pKloc.col));
    System.out.println(String.valueOf(nKloc.row)+String.valueOf(nKloc.col));
    System.out.println(String.valueOf(castleRights.wKs)+String.valueOf(castleRights.wQs)+String.valueOf(castleRights.bKs)+String.valueOf(castleRights.bQs));
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
    if (turn == 1)
    {
      if (move.isCastle)
      {
        if (move.ecol == 6)
        {
          board[5] = 4;
          board[7] = 0;
        }
        else
        {
          board[3] = 4;
          board[0] = 0;
        }
      }
      else if (move.isEnPassent)
      {
        board[32+move.ecol] = 0;
      }
      if (move.pieceMoved == 6)
      {
        pKloc = new Square(move.erow, move.ecol);//positive king location
        castleRights.wKs = false;
        castleRights.wQs = false;
        enPassant = new Square(0,0);
      }
      else if (move.pieceMoved == 1 && move.srow == 1 && move.erow == 3)
      {
        enPassant = new Square(2,move.ecol);
      }
      else
      {
        enPassant = new Square(0,0);
      }
    }
    else
    {
      if (move.isCastle)
      {
        if (move.ecol == 6)
        {
          board[61] = -4;
          board[63] = 0;
        }
        else
        {
          board[59] = -4;
          board[56] = 0;
        }
      }
      else if (move.isEnPassent)
      {
        board[24+move.ecol] = 0;
      }
      if (move.pieceMoved == -6)
      {
        nKloc = new Square(move.erow, move.ecol);
        castleRights.bKs = false;
        castleRights.bQs = false;
        enPassant = new Square(0,0);
      }
      else if (move.pieceMoved == -1 && move.srow == 6 && move.erow == 4)
      {
        enPassant = new Square(5, move.ecol);
      }
      else
      {
        enPassant = new Square(0,0);
      }
    }
    turn = -turn;
    moveLog.add(move);
    castleRightsLog.add(new CastleRights(castleRights.wKs, castleRights.wQs, castleRights.bKs, castleRights.bQs));
    enPassantLog.add(new Square(enPassant.row, enPassant.col));
  }




  public void undoMove()
  {
    var move = moveLog.get(moveLog.size()-1);
    moveLog.remove(moveLog.size()-1);

    var cr = castleRightsLog.get(castleRightsLog.size()-2);
    castleRights = new CastleRights(cr.wKs, cr.wQs, cr.bKs, cr.bQs);
    castleRightsLog.remove(castleRightsLog.size()-1);

    var ep = enPassantLog.get(enPassantLog.size()-2);
    enPassant = new Square(ep.row, ep.col);
    enPassantLog.remove(enPassantLog.size()-1);

    board[move.srow*8+move.scol] = (byte)move.pieceMoved;
    board[move.erow*8+move.ecol] = (byte)move.pieceCaptured;
    if (turn == -1)
    {
      if (move.isCastle)
      {
        if (move.ecol == 6)
        {
          board[5] = 0;
          board[7] = 4;
        }
        else
        {
          board[3] = 0;
          board[0] = 4;
        }
      }
      else if (move.isEnPassent)
      {
        board[32+move.ecol] = -1;
      }
      if (move.pieceMoved == 6)
      {
        pKloc = new Square(move.srow, move.scol);//positive king location
      }
    }
    else
    {
      if (move.isCastle)
      {
        if (move.ecol == 6)
        {
          board[61] = 0;
          board[63] = -4;
        }
        else
        {
          board[59] = 0;
          board[56] = -4;
        }
      }
      else if (move.isEnPassent)
      {
        board[24+move.ecol] = 1;
      }
      if (move.pieceMoved == -6)
      {
        nKloc = new Square(move.srow, move.scol);
      }
    }
    turn = -turn;
  }




  public Move isInValid(String moveID)
  {
    var validMoves = getValidMoves();
    int size = validMoves.size();
    System.out.println("Possible moves: ");
    for (int i = 0; i < size; i++)
    {
      System.out.println("-"+validMoves.get(i).getNotation());
      if (moveID.equals(validMoves.get(i).getNotation()))
      {
        return validMoves.get(i);
      }
    }
    return new Move();
  }
  
  
  public Move getBestMove()
  {
    var validMoves = getValidMoves();
    int eval;
    int bestEval = -1000;
    Move bestMove = new Move();
    if (validMoves.size() == 0)
    {
      System.out.println("Game over, no valid moves found");
    }
    else
    {
      for (int i = 0; i < validMoves.size(); i++)
      {
        Move move = validMoves.get(i);
        makeMove(move);
        eval = -negaMax(startDepth-1, -100, 100);
        undoMove();
        System.out.println(move.getNotation() + " : " + String.valueOf(eval));
        if (eval > bestEval)
        {
          bestMove = validMoves.get(i);
          bestEval = eval;
        }
      }
    }
    System.out.println("BEST MOVE: " + bestMove.getNotation());
    System.out.println("BEST EVAL: " + bestEval);
    return bestMove;
  }

  public int negaMax(int depth, int alpha, int beta)
  {
    if (depth <= 0)
    {
      return evalPos();
    }
    var validMoves = getValidMoves();
    if (validMoves.size() < 0)
    {
      return -1;
    }
    else
    {
      for (int i = 0; i < validMoves.size(); i++)
      {
        makeMove(validMoves.get(i));
        int eval = -negaMax(depth-1, -beta, -alpha);
        undoMove();
        if (eval >= beta)
        {
          return beta;
        }
        if (eval > alpha)
        {
          alpha = eval;
        }
      }
    }
    return alpha;
  }

  int evalPos()
  {
    int eval = 0;
    for (int i = 0; i < 64; i++)
    {
      eval += board[i];
    }
    return eval;
  }
  
  ArrayList<Move> getValidMoves()
  {
    getPinsAndChecks();
    var validMoves = getPossibleMoves();
    if (checks.size() == 1) //if there is only one check, eliminate all moves but those that move king, block check, or eliminate piece giving check
    {
      Square[] check = checks.get(0);
      for (int i = validMoves.size()-1; i >= 0; i--)
      {
        Move move = validMoves.get(i);
        if (Math.abs(board[move.srow*8+move.scol]) != 6)
        {
          if (!isInSquares(new Square(move.erow, move.ecol), check))
          {
            validMoves.remove(i);
          }
        }
      }
      return validMoves;
    }
    else if (checks.size() > 1) //if there is more than one check, eliminate all moves but king moves
    {
      return getKingMoves();
    }
    else 
    {
      return validMoves;
    }
    //afterwards, remove all moves of pinned pieces
  }
  boolean isInSquares(Square square, Square[] arr)
  {
    for (int i = 0; i < arr.length; i++)
    {
      if (arr[i] == null)
      {
        break;
      }
      else if (square.row == arr[i].row && square.col == arr[i].col)
      {
        return true;
      }
    }
    return false;
  }
  ArrayList<Move> getPossibleMoves()
  {
    var validMoves = new ArrayList<Move>();
    for (int row = 0; row < 8; row++)
    {
      for (int col = 0; col < 8; col++)
      {
        int piece = board[row*8+col];
        if (piece != 0)
        {
          if (piece/Math.abs(piece) == turn)
          {
            if (Math.abs(piece) == 1)
            {
              validMoves.addAll(getPawnMoves(row, col));
            }
            else if (Math.abs(piece) == 2)
            {
              validMoves.addAll(getKnightMoves(row, col));
            }
            else if (Math.abs(piece) == 3)
            {
              validMoves.addAll(getDirectionalMoves(row, col, bishopDirections));
            }
            else if (Math.abs(piece) == 4)
            {
              validMoves.addAll(getDirectionalMoves(row, col, rookDirections));
            }
            else if (Math.abs(piece) == 5)
            {
              validMoves.addAll(getDirectionalMoves(row, col, queenDirections));
            }
            else if (Math.abs(piece) == 6)
            {
              validMoves.addAll(getKingMoves());
            }
          }
        }
      }
    }
    return validMoves;
  }
  void getPinsAndChecks()
  {
    pins = new ArrayList<Square[]>();
    checks = new ArrayList<Square[]>();
    Square kingloc = turn == 1 ? pKloc : nKloc;
    for (int i = 0; i < 8; i++)
    {
      Square direction = queenDirections.get(i);
      var foundSquares = new Square[7];
      Square possiblePin = null;
      for (int m = 1; m < 8; m++)
      {
        var sSquare = new Square(direction.row*m+kingloc.row, direction.col*m+kingloc.col);
        foundSquares[m-1] = sSquare;
        if (sSquare.row >= 0 && sSquare.row <= 7 && sSquare.col >= 0 && sSquare.col <= 7)
        {
          int spiece = board[sSquare.row*8+sSquare.col]; //scanner piece
          int type = Math.abs(spiece);
          if (type != 0)
          {
            if (spiece/type == turn)     //0(1,1)    1(1,-1)    2(-1,1)    3(-1,-1)    4(1,0)    5(0,1)    6(-1,0)    7(0,-1)
            {
              if (possiblePin == null) //if its the first run in with a ally piece continue exploring to look for a pin
              {
                possiblePin = sSquare;          
              }
              else //if its the second, stop exploring direction for pins and checks
              {
                break;
              }
            }
            // BECAUSE OF PREVIOUS IF, ALL PAST THIS POINT ARE ENEMY OR EMPTY
            else if (type == 1 && m == 1 && (i >= (turn == 1? 0 : 2) && (i <= (turn == 1? 1 : 3)))) //if there is a pawn, magnitude of 1, in front and diagonal
            {
              checks.add(foundSquares);
              break;
            }
            /*
            the following if statement checks for the following scenarios, in which it would mean that the king is in line of check (not including knights)
            1. there is a queen
            2. there is a bishop, diagonally away
            3. there is a rook horizontally or vertically away
            */
            else if ((type == 5) || (type == 3 && i <= 3) || (type == 4 && i >= 4))
            {
              if (possiblePin == null) //if we have not collided yet
              {
                checks.add(foundSquares);
              }
              else //if we have run into a piece before (we know its an ally piece because otherwise we break)
              {
                pins.add(new Square[]{possiblePin, direction});
              }
              break;
            }
          }
          
        }
        else
        {
          break;
        }
      }
    }

    for (int i = 0; i < 8; i++) //check for knight checks
    {
      int srow = knightMoves.get(i).row + kingloc.row; //scanner row
      int scol = knightMoves.get(i).col + kingloc.col; //scanner col
      if (srow >= 0 && srow <= 7 && scol >= 0 && scol <= 7)
      {
        int spiece = board[srow*8 + scol];
        if (spiece != 0 && spiece/Math.abs(spiece) == -turn && Math.abs(spiece) == 2)
        {
          checks.add(new Square[]{new Square (srow, scol)});
        }
      }
    }
  }
  ArrayList<Move> getPawnMoves(int row, int col)
  {
    boolean piecepinned = false;
    Square pindirection = new Square(10,10);
    for (int i = pins.size()-1; i >= 0; i--)
    {
      var pin = pins.get(i);
      if (pin[0].row == row && pin[0].col == col)
      {
        piecepinned = true;
        pindirection = pin[1];
      }
    }
    var validMoves = new ArrayList<Move>();
    if (board[(row+turn)*8+col] == 0 && (!piecepinned || (pindirection.row == 1*turn && pindirection.col == 0))) //1 forward
    {
      validMoves.add(new Move(row, col, row+turn, col, board));
      if (board[(row+(turn*2))*8+col] == 0 && ((row == 1 && turn == 1)||(row == 6 && turn == -1)))
      {
        validMoves.add(new Move(row, col, row+(turn*2), col, board));
      }
    }
    if ((!piecepinned || (pindirection.row == turn && pindirection.col == 1)) && col+1 <= 7) //forward right capture
    {
      if (board[(row+turn)*8+col+1] != 0 && board[(row+turn)*8+col+1]/Math.abs(board[(row+turn)*8+col+1]) == -turn)//regular capture
        validMoves.add(new Move(row, col, row+turn, col+1, board));
      if (row+turn == enPassant.row && col+1 == enPassant.col)//en passant
      {
        validMoves.add(new Move(row, col, row+turn, col+1, board, false));
      }
    }
    if ((!piecepinned || (pindirection.row == turn && pindirection.col == -1)) && col-1 >= 0) //forward left capture
    {
      if (board[(row+turn)*8+col-1] != 0 && board[(row+turn)*8+col-1]/Math.abs(board[(row+turn)*8+col-1]) == -turn)//regular capture
        validMoves.add(new Move(row, col, row+turn, col-1, board));
      if (row+turn == enPassant.row && col-1 == enPassant.col)//en passant
      {
        validMoves.add(new Move(row, col, row+turn, col-1, board, false));
      }
    }
    return validMoves;
  }
  ArrayList<Move> getKnightMoves(int row, int col)
  {
    var validMoves = new ArrayList<Move>();
    for (int i = pins.size()-1; i >= 0; i--)
    {
      var pin = pins.get(i);
      if (pin[0].row == row && pin[0].col == col)
      {
        return validMoves;
      }
    }
    for (int i = 0; i < 8; i++)
    {
      Square square = knightMoves.get(i);
      int drow = square.row;
      int dcol = square.col;
      if (row+drow >= 0 && row+drow <= 7 && col+dcol >= 0 && col+dcol <= 7)
      {
        int epiece = board[(row+drow)*8+col+dcol];
        if (epiece == 0 || epiece/Math.abs(epiece) == -turn)
        {
          validMoves.add(new Move(row, col, row+drow, col+dcol, board));
        }        
      } 
    }
    return validMoves;
  }
  ArrayList<Move> getKingMoves()
  {
    var validMoves = new ArrayList<Move>();
    Square kingloc = turn == 1 ? pKloc : nKloc;
    board[kingloc.row*8+kingloc.col] = 0;
    for (int i = 0; i < 8; i++)//loop through different thetas, aka directions that we are exploring
    {
      int drow = queenDirections.get(i).row + kingloc.row;
      int dcol = queenDirections.get(i).col + kingloc.col;
      if (drow >= 0 && drow <= 7 && dcol >= 0 && dcol <= 7)
      {
        int epiece = board[(drow)*8+dcol];
        if ((epiece == 0 || epiece/Math.abs(epiece) == -turn) && !isAttacked(drow, dcol))
        {
          board[kingloc.row*8+kingloc.col] = (byte)(turn * 6);
          var move = new Move(kingloc.row, kingloc.col, drow, dcol, board);
          board[kingloc.row*8+kingloc.col] = 0;
          validMoves.add(move);
        }
      }
    }
    board[kingloc.row*8+kingloc.col] = (byte)(turn * 6);
    validMoves.addAll(getCastleMoves());
    return validMoves;
  }

  ArrayList<Move> getCastleMoves()
  {
    Square kingloc = turn == 1 ? pKloc : nKloc;
    var validMoves = new ArrayList<Move>();
    if (checks.size() == 0)
    {
      if (turn == 1)
      {
        if (castleRights.wKs)
        {
          if (board[5] == 0 && board[6] == 0 && !isAttacked(0, 5) && !isAttacked(0, 6))
          {
            validMoves.add(new Move(kingloc.row, kingloc.col, 0, 6, board, true));
          }
        }
        if (castleRights.wQs)
        {
          if (board[1] == 0 && board[2] == 0 && board[3] == 0 && !isAttacked(0, 1) && !isAttacked(0, 2) && !isAttacked(0, 3))
          {
            validMoves.add(new Move(kingloc.row, kingloc.col, 0, 2, board, true));
          }
        }
      }
      else
      {
        if (castleRights.bKs)
        {
          if (board[61] == 0 && board[62] == 0 && !isAttacked(7, 5) && !isAttacked(7, 6))
          {
            validMoves.add(new Move(kingloc.row, kingloc.col, 7, 6, board, true));
          }
        }
        if (castleRights.bQs)
        {
          if (board[57] == 0 && board[58] == 0 && board[59] == 0 && !isAttacked(8, 2) && !isAttacked(8, 3))
          {
            validMoves.add(new Move(kingloc.row, kingloc.col, 7, 2, board, true));
          }
        }
      }
    }
    return validMoves;
  }

  boolean isAttacked(int row, int col)
  {
    for (int i = 0; i < 8; i++)
    {
      Square direction = queenDirections.get(i);
      for (int m = 1; m < 8; m++)
      {
        var sSquare = new Square(direction.row*m+row, direction.col*m+col);
        if (sSquare.row >= 0 && sSquare.row <= 7 && sSquare.col >= 0 && sSquare.col <= 7)
        {
          int spiece = board[sSquare.row*8+sSquare.col]; //scanner piece
          int type = Math.abs(spiece);
          if (type != 0)
          {
            if (spiece/type == turn)
            {
              break;
            }
            else if ((type == 1 && m == 1 && (i >= (turn == 1? 0 : 2) && (i <= (turn == 1? 1 : 3)))) || (type == 5) || (type == 3 && i <= 3) || (type == 4 && i >= 4))
            {
              return true;
            }
            else
            {
              break;
            }
          }
        }
        else
        {
          break;
        }
      }
    }
    for (int i = 0; i < 8; i++)
    {
      int srow = knightMoves.get(i).row+row;
      int scol = knightMoves.get(i).col+col;
      if (srow >= 0 && srow <= 7 && scol >= 0 && scol <= 7 && board[srow*8+scol] == -turn*2)
      {
        return true;
      }
    }
    return false;
  }
  ArrayList<Move> getDirectionalMoves(int row, int col, ArrayList<Square> directions)
  {
    boolean piecepinned = false;
    Square pindirection = new Square(10,10);
    for (int i = pins.size()-1; i >= 0; i--)
    {
      var pin = pins.get(i);
      if (pin[0].row == row && pin[0].col == col)
      {
        piecepinned = true;
        pindirection = pin[1];
      }
    }
  
    var validMoves = new ArrayList<Move>();
    for (int i = 0; i < directions.size(); i++)//theta, aka direction that we are exploring (just saying theta bc im cool)
    {
      for (int m = 1; m < 8; m++)//m for magnitude (again precalc terminology hehe)
      {
        int drow = directions.get(i).row*m;
        int dcol = directions.get(i).col*m;
        if (row+drow >= 0 && row+drow <= 7 && col+dcol >= 0 && col+dcol <= 7 && (!piecepinned || (pindirection.row == directions.get(i).row && pindirection.col == directions.get(i).col)))
        {
          int epiece = board[(row+drow)*8+col+dcol];
          if (epiece == 0)
          {
            validMoves.add(new Move(row, col, row+drow, col+dcol, board));
          }
          else if (epiece/Math.abs(epiece) == -turn)
          {
            validMoves.add(new Move(row, col, row+drow, col+dcol, board));
            break;
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
}


class Move
{
  public int srow;
  public int scol;
  public int erow;
  public int ecol;
  public boolean isCastle;
  public boolean isEnPassent;
  public int pieceMoved;
  public int pieceCaptured;
  public String moveID = "";
  public static HashMap<Integer, String> intToLetter = new HashMap<Integer, String>(){{
    put(0,"a");
    put(1,"b");
    put(2,"c");
    put(3,"d");
    put(4,"e");
    put(5,"f");
    put(6,"g");
    put(7,"h");
  }};
  public static HashMap<String, Integer> letterToInt = new HashMap<String, Integer>(){{
    put("a",0);
    put("b",1);
    put("c",2);
    put("d",3);
    put("e",4);
    put("f",5);
    put("g",6);
    put("h",7);
  }};
  Move(int startRow, int startCol, int endRow, int endCol, byte[] board)
  {
    srow = startRow;
    scol = startCol;
    erow = endRow;
    ecol = endCol;
    moveID = String.valueOf(startRow) + String.valueOf(startCol) + String.valueOf(endRow) + String.valueOf(endCol);
    pieceMoved = board[srow*8+scol];
    pieceCaptured = board[erow*8+ecol];
  }
  Move(int startRow, int startCol, int endRow, int endCol, byte[] board, boolean isSpecial)//CASTLE BOOLEAN = true, EN PASSANT BOOLEAN = false
  {
    srow = startRow;
    scol = startCol;
    erow = endRow;
    ecol = endCol;
    isCastle = isSpecial;
    isEnPassent = !isSpecial;
    moveID = String.valueOf(startRow) + String.valueOf(startCol) + String.valueOf(endRow) + String.valueOf(endCol);
    pieceMoved = board[srow*8+scol];
    pieceCaptured = board[erow*8+ecol];
  }
  Move(String notation, byte[] board)
  {
    srow = letterToInt.get(notation.substring(0,1));
    scol = Integer.parseInt(notation.substring(1,2));
    erow = letterToInt.get(notation.substring(2,3));
    ecol = Integer.parseInt(notation.substring(3,4));
    moveID = String.valueOf(srow) + String.valueOf(scol) + String.valueOf(srow) + String.valueOf(scol);
    pieceMoved = board[srow*8+scol];
    pieceCaptured = board[erow*8+ecol];
  }
  Move()
  {

  }
  String getNotation()
  {
    return intToLetter.get(scol) + String.valueOf(srow+1) + intToLetter.get(ecol) + String.valueOf(erow+1);
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
class CastleRights
{
  public boolean wKs = false;
  public boolean wQs = false;
  public boolean bKs = false;
  public boolean bQs = false;
  CastleRights(boolean iwKs, boolean iwQs, boolean ibKs, boolean ibQs)
  {
    wKs = iwKs;
    wQs = iwQs;
    bKs = ibKs;
    bQs = ibQs;
  }
}