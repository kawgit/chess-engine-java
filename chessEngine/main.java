import java.util.HashMap;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Random;
class Main {
  public static void main(String[] args) {


    var gs = new gameState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", 1, 12, 300);
    //unsolved "r4rk1/1b3p1p/p1q1pQp1/6RP/8/8/PPp2PP1/R5K1 w - - 0 1" estimated depth 10
    // "rr3k2/4n3/1q1p2p1/2pP2Q1/5PP1/1p6/p3N3/K6R w - - 0 38"

    boolean running = true;

    Scanner scanner = new Scanner(System.in);
    while (running)//player vs player or player vs ai format
    {
      if (gs.turn == gs.aiMode)
      {
        long start = System.currentTimeMillis();
        gs.makeMove(gs.getBestMove());
        long finish = System.currentTimeMillis();
        System.out.println("Time elapsed: " + String.valueOf((double)(finish - start)/1000));
      }
      else 
      {
        gs.nValidMoves = gs.getValidMoves();
      }
      gs.printBoard();
      System.out.println("enter move: \n");
      String input = scanner.nextLine();
      if (input.length() == 4)
      {
        Move move = gs.isInValid(input);
        if (move.srow != -5)
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
      else if (input.equals("ai_mode"))
      {
        input = scanner.nextLine();
        gs.aiMode = Integer.parseInt(input);
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
  public int maxDepth;
  public int maxCalcTime;
  public int collisions;

  HashMap<Integer, String> boardPrintKey = new HashMap<Integer, String>();
  HashMap<String, Integer> fenKey = new HashMap<String, Integer>();
  HashMap<Byte, Integer> pieceToEval = new HashMap<Byte, Integer>();

  ArrayList<Square> knightMoves = new ArrayList<Square>();
  ArrayList<Square> bishopDirections = new ArrayList<Square>();
  ArrayList<Square> rookDirections = new ArrayList<Square>();
  ArrayList<Square> queenDirections = new ArrayList<Square>();
  ArrayList<Square> kingMoves = new ArrayList<Square>();
  ArrayList<Square[]> pins = new ArrayList<Square[]>();
  ArrayList<Square[]> checks = new ArrayList<Square[]>();
  ArrayList<Move> moveLog = new ArrayList<Move>();
  ArrayList<Move> nValidMoves = new ArrayList<Move>();
  ArrayList<Move> pValidMoves = new ArrayList<Move>();
  ArrayList<CastleRights> castleRightsLog = new ArrayList<CastleRights>();
  ArrayList<Integer> enPassantLog = new ArrayList<Integer>();
  ArrayList<FoundEval> transpositionTable = new ArrayList<FoundEval>();

  Square pKloc; //positive king location
  Square nKloc; //negative king location
  int enPassant = -1; //possible location of enpassant capture (erow, ecol)
  int makeCount = 0;
  int undoCount = 0;
  CastleRights castleRights = new CastleRights(false, false, false, false);



  gameState(String fen, int iAiMode, int iMaxDepth, int iMaxCalcTime)
  {
    aiMode = iAiMode;
    maxDepth = iMaxDepth;
    maxCalcTime = iMaxCalcTime;

    //constructed here because it is needed for crreation of board
    fenKey.put("p", 7);
    fenKey.put("n", 8);
    fenKey.put("b", 9);
    fenKey.put("r", 10);
    fenKey.put("q", 11);
    fenKey.put("k", 12);
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
    if (turn == 1)
    {
      pValidMoves = getValidMoves();
    }
    else 
    {
      nValidMoves = getValidMoves();
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
    boardPrintKey.put(7,"bP");
    boardPrintKey.put(8,"bN");
    boardPrintKey.put(9,"bB");
    boardPrintKey.put(10,"bR");
    boardPrintKey.put(11,"bQ");
    boardPrintKey.put(12,"bK");
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

    pieceToEval.put((byte)0,0);
    pieceToEval.put((byte)1,1);
    pieceToEval.put((byte)2,3);
    pieceToEval.put((byte)3,3);
    pieceToEval.put((byte)4,5);
    pieceToEval.put((byte)5,9);
    pieceToEval.put((byte)6,0);
    pieceToEval.put((byte)7,-1);
    pieceToEval.put((byte)8,-3);
    pieceToEval.put((byte)9,-3);
    pieceToEval.put((byte)10,-5);
    pieceToEval.put((byte)11,-9);
    pieceToEval.put((byte)12,0);
  

    for (int row = 0; row < 8; row++)
    {
      for (int col = 0; col < 8; col++)
      {
        if (board[row*8+col] == 6)
        {
          pKloc = new Square(row, col);
        }
        else if (board[row*8+col] == 12)
        {
          nKloc = new Square(row, col);
        }
      }
    }
    castleRightsLog.add(new CastleRights(castleRights.wKs, castleRights.wQs, castleRights.bKs, castleRights.bQs));
    enPassantLog.add(enPassant);

    Random rand = new Random(432738243); // 421948721 
    int[] irngMap = new int[844]; //(OBJECTIVE IS TO CREATE KEYS THAT ALMOST NEVER OVERLAP) 1 number for each possible peice on each possible square + 1 for empty(13 x 64), 1 number for each castleRights boolean (4), 1 number for each possible enpassant column(8)
    for (int i = 0; i < 844; i++) //negative or positive for turn
    {
      irngMap[i] = rand.nextInt(); //int.max / 844 = 2544411
    }
    FoundEval.rngMap = irngMap;
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
  public int getColor(int piece)
  {
    return piece <= 6? (piece == 0? 0 : 1) : -1;
  }
  public int getType(int piece)
  {
    return piece <= 6? piece : piece - 6;
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
    makeCount++;
    board[move.erow*8+move.ecol] = (byte)move.pieceMoved;
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
      else if (move.isPawnPromotion)
      {
        board[move.erow*8+move.ecol] = (byte)5;
      }
      if (move.pieceMoved == 6)
      {
        pKloc = new Square(move.erow, move.ecol);//positive king location
        castleRights.wKs = false;
        castleRights.wQs = false;
        enPassant = -1;
      }
      else if (move.pieceMoved == 1 && move.srow == 1 && move.erow == 3)
      {
        enPassant = move.ecol;
      }
      else
      {
        enPassant = -1;
      }
    }
    else
    {
      if (move.isCastle)
      {
        if (move.ecol == 6)
        {
          board[61] = 10;
          board[63] = 0;
        }
        else
        {
          board[59] = 10;
          board[56] = 0;
        }
      }
      else if (move.isEnPassent)
      {
        board[24+move.ecol] = 0;
      }
      else if (move.isPawnPromotion)
      {
        board[move.erow*8+move.ecol] = (byte)11;
      }
      if (move.pieceMoved == 12)
      {
        nKloc = new Square(move.erow, move.ecol);
        castleRights.bKs = false;
        castleRights.bQs = false;
        enPassant = -1;
      }
      else if (move.pieceMoved == 7 && move.srow == 6 && move.erow == 4)
      {
        enPassant = move.ecol;
      }
      else
      {
        enPassant = -1;
      }
    }
    turn = -turn;
    moveLog.add(move);
    castleRightsLog.add(new CastleRights(castleRights.wKs, castleRights.wQs, castleRights.bKs, castleRights.bQs));
    enPassantLog.add(enPassant);
  }
  public void undoMove()
  {
    undoCount++;
    var move = moveLog.get(moveLog.size()-1);
    moveLog.remove(moveLog.size()-1);

    var cr = castleRightsLog.get(castleRightsLog.size()-2);
    castleRights = new CastleRights(cr.wKs, cr.wQs, cr.bKs, cr.bQs);
    castleRightsLog.remove(castleRightsLog.size()-1);

    enPassant = enPassantLog.get(enPassantLog.size()-2);
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
        board[32+move.ecol] = 7;
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
          board[63] = 10;
        }
        else
        {
          board[59] = 0;
          board[56] = 10;
        }
      }
      else if (move.isEnPassent)
      {
        board[24+move.ecol] = 1;
      }
      if (move.pieceMoved == 12)
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
        if (move.pieceMoved != 6 && move.pieceMoved != 12)
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
    if (turn == 1)
    {
      for (int row = 0; row < 8; row++)
      {
        for (int col = 0; col < 8; col++)
        {
          int piece = board[row*8+col];
          if (getColor(piece) == 1) //IN FUTURE TRY TO INCOPERATE MAP OF LAMBDA INSTEAD OF THIS MESS, NOT SURE HOW TO THO
          {
            if (piece == 1)
            {
              validMoves.addAll(getPawnMoves(row, col));
            }
            else if (piece == 2)
            {
              validMoves.addAll(getKnightMoves(row, col));
            }
            else if (piece == 3)
            {
              validMoves.addAll(getDirectionalMoves(row, col, bishopDirections));
            }
            else if (piece == 4)
            {
              validMoves.addAll(getDirectionalMoves(row, col, rookDirections));
            }
            else if (piece == 5)
            {
              validMoves.addAll(getDirectionalMoves(row, col, queenDirections));
            }
            else if (piece == 6)
            {
              validMoves.addAll(getKingMoves());
            }
          }
        }
      }      
    }
    else
    {
      for (int row = 0; row < 8; row++)
      {
        for (int col = 0; col < 8; col++)
        {
          int piece = board[row*8+col];
          if (getColor(piece) == -1) //IN FUTURE TRY TO INCOPERATE MAP OF LAMBDA INSTEAD OF THIS MESS, NOT SURE HOW TO THO
          {
            if (piece ==  7)
            {
              validMoves.addAll(getPawnMoves(row, col));
            }
            else if (piece == 8)
            {
              validMoves.addAll(getKnightMoves(row, col));
            }
            else if (piece == 9)
            {
              validMoves.addAll(getDirectionalMoves(row, col, bishopDirections));
            }
            else if (piece == 10)
            {
              validMoves.addAll(getDirectionalMoves(row, col, rookDirections));
            }
            else if (piece == 11)
            {
              validMoves.addAll(getDirectionalMoves(row, col, queenDirections));
            }
            else if (piece == 12)
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
          int type = getType(spiece);
          if (type != 0)
          {
            if (getColor(spiece) == turn)     //0(1,1)    1(1,-1)    2(-1,1)    3(-1,-1)    4(1,0)    5(0,1)    6(-1,0)    7(0,-1)
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

    for (int i = 0; i < 8; i++) //check for knight checks
    {
      int srow = knightMoves.get(i).row + kingloc.row; //scanner row
      int scol = knightMoves.get(i).col + kingloc.col; //scanner col
      if (srow >= 0 && srow <= 7 && scol >= 0 && scol <= 7)
      {
        int spiece = board[srow*8 + scol];
        if (getColor(spiece) == -turn && getType(spiece) == 2)
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
    if (board[(row+turn)*8+col] == 0 && (!piecepinned || (pindirection.row == turn && pindirection.col == 0))) //1 forward
    {
      validMoves.add(new Move(row, col, row+turn, col, board));
      if (((row == 1 && turn == 1)||(row == 6 && turn == -1)) && board[(row+(turn*2))*8+col] == 0)
      {
        validMoves.add(new Move(row, col, row+(turn*2), col, board));
      }
    }

    if ((!piecepinned || (pindirection.row == turn && pindirection.col == 1)) && col <= 6) //forward right capture
    {
      if (getColor(board[(row+turn)*8+col+1]) == -turn)//regular capture
        validMoves.add(new Move(row, col, row+turn, col+1, board));
      if (col+1 == enPassant && ((row == 4 && turn == 1) || (row == 3 && turn == -1)))//en passant
      {
        validMoves.add(new Move(row, col, row+turn, col+1, board, false));
      }
    }
    if ((!piecepinned || (pindirection.row == turn && pindirection.col == -1)) && col >= 1) //forward left capture
    {
      if (getColor(board[(row+turn)*8+col-1]) == -turn)//regular capture
        validMoves.add(new Move(row, col, row+turn, col-1, board));
      if (col-1 == enPassant && ((row == 4 && turn == 1) || (row == 3 && turn == -1)))//en passant
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
        if (epiece == 0 || getColor(epiece) == -turn)
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
        if ((epiece == 0 || getColor(epiece) == -turn) && !isAttacked(drow, dcol))
        {
          board[kingloc.row*8+kingloc.col] = (byte)(turn == 1? 6 : 12);
          var move = new Move(kingloc.row, kingloc.col, drow, dcol, board);
          board[kingloc.row*8+kingloc.col] = 0;
          validMoves.add(move);
        }
      }
    }
    board[kingloc.row*8+kingloc.col] = (byte)(turn == 1? 6 : 12);
    validMoves.addAll(getCastleMoves());
    return validMoves;
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
          else if (getColor(epiece) == -turn)
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
          int type = getType(spiece);
          if (type != 0)
          {
            if (getColor(spiece) == turn)
            {
              break;
            }
            else if ((type == 1 && m == 1 && (i >= (turn == 1? 0 : 2) && (i <= (turn == 1? 1 : 3)))) || (type == 5) || (type == 3 && i <= 3) || (type == 4 && i >= 4) || (type == 6 && m == 1))
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
      if (srow >= 0 && srow <= 7 && scol >= 0 && scol <= 7)
      {
        int spiece = board[srow*8+scol];
        if (getColor(spiece) == -turn && getType(spiece) == 2)
        {
        return true;          
        }
      }
    }
    return false;
  }

  public Move getBestMove()
  {
    makeCount = 0;
    undoCount = 0;
    System.out.println("\n\n\n");
    int bestEval = -100000;
    Move bestMove = new Move();
    long start = System.currentTimeMillis();
    for (int depth = 2; (depth<=maxDepth && ((System.currentTimeMillis() - start)/1000 < maxCalcTime)); depth++)
    {
      bestEval = -100000;
      bestMove = new Move();
      var orderedMoves = new ArrayList<Move>();
      var validMoves = new ArrayList<Move>();
      FoundEval thisEval = new FoundEval(depth, 0, board, turn, castleRights, enPassant);
      int r = isFoundEval(thisEval);
      if (r != -1)
      {
        validMoves = transpositionTable.get(r).orderedMoves;
      }
      else 
      {
        System.out.println("DID NOT USE ORDERED");
        validMoves = getValidMoves();
      }
      if (validMoves.size() == 0)
      {
        System.out.println("Game over, no valid moves found");
      }
      else
      {
        System.out.println("Move | Eval | Calculation time | Depth");
        System.out.println("—————————————————————————————————————————");
        for (int i = 0; i < validMoves.size(); i++)
        {
          long startt = System.currentTimeMillis();
          int eval;
          Move move = validMoves.get(i);
          makeMove(move);
          eval = -negaMax(depth-1, -100000, 100000);
          move.eval = eval;
          insertOrderedMove(move, orderedMoves);
          undoMove();
          System.out.println(move.getNotation() + " | " + (eval < 0? "" : " ") + (Math.abs(eval) > 9? "" : " ") + (Math.abs(eval) > 99? "" : " ") + String.valueOf(eval) + " |    " + String.valueOf(((double)System.currentTimeMillis()-startt)/1000) + " seconds |     " + String.valueOf(depth));
          if (eval > bestEval)
          {
            bestMove = validMoves.get(i);
            bestEval = eval;
            if (bestEval >= 1000)
            {
              System.out.println("FOUND");
              break;
            }
          }
        }
        if (thisEval.board != (new FoundEval(depth, 0, board, turn, castleRights, enPassant)).board)
        {
          System.out.println("THERE WAS PROBABLY A HASH COLLISION");
          System.exit(0);
        }
      }
      insertFoundEval(new FoundEval(depth, bestEval, board, turn, castleRights, enPassant, orderedMoves));
    }
    System.out.println("Best Move                | " + bestMove.getNotation());
    System.out.println("Best Evaluation          | " + bestEval);
    System.out.println("Transposition table size | " + String.valueOf(transpositionTable.size()));
    System.out.println("Transposition collisions | " + String.valueOf(collisions));
    System.out.println("Collision rate(% of pos) | " + String.valueOf((double)collisions/transpositionTable.size()));
    collisions = 0;
    return bestMove;
  }
  public int negaMax(int depth, int alpha, int beta)
  {
    var orderedMoves = new ArrayList<Move>();
    var validMoves = new ArrayList<Move>();
    var boardSave = board.clone();
    if (depth <= 0)
    {
      return evalPos();
    }
    else
    {
      FoundEval thisEval = new FoundEval(depth, 0, board, turn, castleRights, enPassant);
      int r = isFoundEval(thisEval);
      if (r != -1)
      {
        if (transpositionTable.get(r).depth >= depth)
        {
          return transpositionTable.get(r).eval;          
        }
        else 
        {
          validMoves = transpositionTable.get(r).orderedMoves;
        }
      }
      else
      {
        validMoves = getValidMoves();
      }
      if (validMoves.size() == 0)
      {
        if (checks.size() == 0)
        {
          return 0;    
        }
        else
        {
          return -1000;
        }
      }
      for (int i = 0; i < validMoves.size(); i++)
      {
        Move move = validMoves.get(i);
        makeMove(move);
        int eval = -negaMax(depth-1, -beta, -alpha);
        move.eval = eval;
        insertOrderedMove(move, orderedMoves);
        undoMove();
        if (thisEval.board != (new FoundEval(depth, 0, board, turn, castleRights, enPassant)).board)
        {
          board = boardSave.clone();
          i = 0;
          orderedMoves.clear();
          validMoves.clear();
          if (r != -1)
          {
            transpositionTable.remove(r);
          }
          collisions++;
          return safeNegaMax(depth, alpha, beta);
        }
        if (eval > alpha)
        {
          alpha = eval;
        }
        if (alpha >= beta)
        {
          orderedMoves.addAll(validMoves.subList(i+1, validMoves.size()));
          break;
        }
      }
    }
    insertFoundEval(new FoundEval(depth, alpha, board, turn, castleRights, enPassant, orderedMoves)); //insert to transposition table
    return alpha;
  }
  int safeNegaMax(int depth, int alpha, int beta) //does not use transposition table, used as back up to regular negamax in case of collision.
  {
    var validMoves = new ArrayList<Move>();
    if (depth <= 0)
    {
      return evalPos();
    }
    else
    {
      validMoves = getValidMoves();
      if (validMoves.size() == 0)
      {
        if (checks.size() == 0)
        {
          return 0;    
        }
        else
        {
          return -1000*depth;
        }
      }
      for (int i = 0; i < validMoves.size(); i++)
      {
        Move move = validMoves.get(i);
        makeMove(move);
        int eval = -negaMax(depth-1, -beta, -alpha);
        undoMove();
        if (eval > alpha)
        {
          alpha = eval;
        }
        if (alpha >= beta)
        {
          break;
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
      eval += pieceToEval.get(board[i]);
    }
    return eval*turn;      
  }
  void insertFoundEval(FoundEval foundEval)
  {
    int left = 0;
    int right = transpositionTable.size()-1;
    long prevBoard = 0;
    if (right != -1)
    {
      while (true)
      {
        var scan = transpositionTable.get(Math.round((left+right)/2));
        if (foundEval.board == scan.board)
        {
          transpositionTable.set(Math.round((left+right)/2), foundEval);
          break;
        }
        else if (foundEval.board < scan.board)
        {
          right = Math.round((left+right)/2);
        }
        else if (foundEval.board > scan.board)
        {
          left = Math.round((left+right)/2);
        }
        if  (scan.board != prevBoard)
        {
          prevBoard = scan.board;
        }
        else
        {
          if (foundEval.board > scan.board)
          {
            transpositionTable.add((int)Math.ceil((left+right)/2)+1, foundEval);
          }
          else
          {
            transpositionTable.add((int)Math.ceil((left+right)/2), foundEval);
          }
          break;
        }
      }
    }
    else 
    {
      transpositionTable.add(foundEval);
    }
  }
  int isFoundEval(FoundEval foundEval) //returns -1 if false, else returns index of found eval in transposition table
  {
    int left = 0;
    int right = transpositionTable.size()-1;
    long prevBoard = 0;
    if (right != -1)
    {
      while (true)
      {
        long scan = transpositionTable.get(Math.round((left+right)/2)).board;
        if (scan == foundEval.board)
        {
          return Math.round((left+right)/2);
        }
        else if (scan < foundEval.board)
        {
          left = Math.round((left+right)/2);
        }
        else if (scan > foundEval.board)
        {
          right = Math.round((left+right)/2);
        }
        if  (scan != prevBoard)
        {
          prevBoard = scan;
        }
        else
        {
          return -1;
        }
      }
    }
    return -1;
  }
  void insertOrderedMove(Move move, ArrayList<Move> list)
  {
    int left = 0;
    int right = list.size()-1;
    int prev = 0;
    if (right != -1)
    {
      while (true)
      {
        var scan = list.get(Math.round((left+right)/2)).eval;
        if (move.eval == scan)
        {
          list.add((int)Math.ceil((left+right)/2), move);
          break;
        }
        else if (move.eval > scan)
        {
          right = Math.round((left+right)/2);
        }
        else if (move.eval < scan)
        {
          left = Math.round((left+right)/2);
        }
        if  (scan != prev)
        {
          prev = scan;
        }
        else
        {
          if (move.eval > scan)
          {
            list.add((int)Math.ceil((left+right)/2), move);
          }
          else
          {
            list.add((int)Math.ceil((left+right)/2)+1, move);
          }
          break;
        }
      }
    }
    else 
    {
      list.add(move);
    }
  }
}


class Move
{
  public byte srow;
  public byte scol;
  public byte erow;
  public byte ecol;
  public byte pieceMoved;
  public byte pieceCaptured;
  public int eval;
  public boolean isCastle;
  public boolean isEnPassent;
  public boolean isPawnPromotion;

  public boolean isEqual(Move move)
  {
    return (move.srow == srow && move.scol == scol && move.erow == erow && move.ecol == ecol)? true : false;
  }

  public static HashMap<Byte, String> byteToLetter = new HashMap<Byte, String>(){{
    put((byte)0,"a");
    put((byte)1,"b");
    put((byte)2,"c");
    put((byte)3,"d");
    put((byte)4,"e");
    put((byte)5,"f");
    put((byte)6,"g");
    put((byte)7,"h");
  }};
  public static HashMap<String, Byte> letterToByte = new HashMap<String, Byte>(){{
    put("a",(byte)0);
    put("b",(byte)1);
    put("c",(byte)2);
    put("d",(byte)3);
    put("e",(byte)4);
    put("f",(byte)5);
    put("g",(byte)6);
    put("h",(byte)7);
  }};
  Move(int startRow, int startCol, int endRow, int endCol, byte[] board)
  {
    srow = (byte)startRow;
    scol = (byte)startCol;
    erow = (byte)endRow;
    ecol = (byte)endCol;
    pieceMoved = board[srow*8+scol];
    pieceCaptured = board[erow*8+ecol];
    if ((pieceMoved <= 6? pieceMoved : pieceMoved - 6) == 1 && (erow == 7 || erow == 0))
    {
      isPawnPromotion = true;
    } 
  }
  Move(int startRow, int startCol, int endRow, int endCol, byte[] board, boolean isSpecial)//CASTLE BOOLEAN = true, EN PASSANT BOOLEAN = false
  {
    srow = (byte)startRow;
    scol = (byte)startCol;
    erow = (byte)endRow;
    ecol = (byte)endCol;
    isCastle = isSpecial;
    isEnPassent = !isSpecial;
    pieceMoved = board[srow*8+scol];
    pieceCaptured = board[erow*8+ecol];
  }
  Move(String notation, byte[] board)
  {
    srow = (byte)letterToByte.get(notation.substring(0,1));
    scol = (byte)Integer.parseInt(notation.substring(1,2));
    erow = (byte)letterToByte.get(notation.substring(2,3));
    ecol = (byte)Integer.parseInt(notation.substring(3,4));
    pieceMoved = board[srow*8+scol];
    pieceCaptured = board[erow*8+ecol];
    if ((pieceMoved <= 6? pieceMoved : pieceMoved - 6) == 1 && (erow == 7 || erow == 0))
    {
      isPawnPromotion = true;
    } 
  }
  Move()
  {
    srow = (byte)-5;
  }
  String getNotation()
  {
    return byteToLetter.get(scol) + String.valueOf(srow+1) + byteToLetter.get(ecol) + String.valueOf(erow+1);
  }
}
class Square
{
  public byte row;
  public byte col;
  Square(int irow, int icol)
  {
    row = (byte)irow;
    col = (byte)icol;
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
class FoundEval
{
  public int depth;
  public int eval;
  public long board;
  public static int[] rngMap;
  public ArrayList<Move> orderedMoves = new ArrayList<Move>();
  FoundEval(int idepth, int ieval, byte[] iboard, int iturn, CastleRights icastleRights, int ienPassant)
  {
    depth = idepth;
    eval = ieval;
    for (int i = 0; i < 64; i++)
    {
      board = board ^ rngMap[i*13+iboard[i]];
    }
    board = board ^ (icastleRights.wKs? 1 : 0) * rngMap[832] + (icastleRights.wQs? 1 : 0) * rngMap[833] + (icastleRights.bKs? 1 : 0) * rngMap[834] + (icastleRights.bQs? 1 : 0) * rngMap[835];
    board = board ^ rngMap[836+ienPassant];
    board = board ^ iturn;
  }
  FoundEval(int idepth, int ieval, byte[] iboard, int iturn, CastleRights icastleRights, int ienPassant, ArrayList<Move >iorderedMoves)
  {
    depth = idepth;
    eval = ieval;
    for (int i = 0; i < 64; i++)
    {
      board = board ^ rngMap[i*13+iboard[i]];
    }
    board = board ^ (icastleRights.wKs? 1 : 0) * rngMap[832] + (icastleRights.wQs? 1 : 0) * rngMap[833] + (icastleRights.bKs? 1 : 0) * rngMap[834] + (icastleRights.bQs? 1 : 0) * rngMap[835];
    board = board ^ rngMap[836+ienPassant];
    board = board ^ iturn;
    orderedMoves = iorderedMoves;
  }
}