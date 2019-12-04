package com.syntech.chess.logic;

import com.syntech.chess.graphic.CellGraphics;
import com.syntech.chess.graphic.Color;
import com.syntech.chess.logic.pieces.Piece;
import com.syntech.chess.rules.MovePriorities;
import com.syntech.chess.rules.MovementRules;
import com.syntech.chess.rules.chess.DoublePawnType;
import com.syntech.chess.text.Translation;
import org.ice1000.jimgui.JImGui;
import org.ice1000.jimgui.JImGuiGen;
import org.ice1000.jimgui.JImStyleColors;
import org.ice1000.jimgui.NativeBool;
import org.ice1000.jimgui.flag.JImWindowFlags;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;

//TODO: add the ability to import PGN files

public class Board implements Cloneable {
    private static final Point pieceNone = new Point(-1, -1);
    protected int width, height;
    protected ArrayList<Point> pieces;
    protected ArrayList<Point> movablePieces = new ArrayList<>();
    protected ArrayList<Move> availableMoves = new ArrayList<>();
    protected ArrayList<Move> availableCaptures = new ArrayList<>();
    private Board previousBoard = null;
    private Piece[][] board;
    private float windowWidth, windowHeight;
    private Side turnIndicator = Side.WHITE;
    private boolean displayPromotionPopup = false;
    private boolean displayResultPopup = false;
    private String status;
    private Side statusSide = Side.WHITE;
    private PieceType statusPiece = PieceType.PAWN;
    private boolean gameEnded;
    private Translation translation;
    private Point selectedPiece = new Point(-1, -1);
    private Point enPassantPointWhite = new Point(-1, -1);
    private Point enPassantPointBlack = new Point(-1, -1);
    protected int turn;
    private ArrayList<Move> moveLog = new ArrayList<>();

    private Board(@NotNull Piece[][] board, Translation translation, boolean initialize, boolean update, int turn) {
        this.translation = translation;
        this.turn = turn;
        height = board.length;
        width = board[0].length;
        this.board = new Piece[height][width];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                try {
                    this.board[row][col] = (Piece) board[row][col].clone();
                } catch (CloneNotSupportedException ignored) {
                    this.board[row][col] = board[row][col];
                }
            }
        }
        if (initialize) {
            for (int row = 0; row < height; row++) {
                for (int col = 0; col < width; col++) {
                    this.board[row][col].setPosition(row, col);
                }
            }
        }
        updatePieces();
        if (update) {
            updateMovablePieces();
        }
    }

    public Board(@NotNull Piece[][] board, Translation translation, boolean initialize, boolean update) {
        this(board, translation, initialize, update, 0);
    }

    public Board(@NotNull Piece[][] board, Translation translation, int turn) {
        this(board, translation, false, false, turn);
    }


    @Override
    public Object clone() throws CloneNotSupportedException {
        Board clone = (Board) super.clone();
        Board copy = new Board(board, translation, turn);
        clone.board = copy.board;
        clone.previousBoard = previousBoard;
        clone.selectedPiece = pieceNone;
        clone.enPassantPointWhite = new Point(enPassantPointWhite);
        clone.enPassantPointBlack = new Point(enPassantPointBlack);
        return clone;
    }

    public Piece[][] getBoard() {
        return board;
    }

    protected Translation getTranslation() {
        return translation;
    }

    public void setTranslation(Translation translation) {
        this.translation = translation;
        status = getStatusConditions(getTurnSide());
    }

    public void display(@NotNull JImGui imGui, String name, float size) {

        float spacingX = imGui.getStyle().getItemSpacingX();
        float spacingY = imGui.getStyle().getItemSpacingY();
        float paddingX = imGui.getStyle().getFramePaddingX();
        float paddingY = imGui.getStyle().getFramePaddingY();

        imGui.getStyle().setItemSpacingX(0);
        imGui.getStyle().setItemSpacingY(0);
        imGui.getStyle().setFramePaddingX(0);
        imGui.getStyle().setFramePaddingY(0);

        imGui.begin(name, new NativeBool(), JImWindowFlags.NoMove | JImWindowFlags.NoTitleBar | JImWindowFlags.AlwaysAutoResize);
        displayLabelRow(imGui, size);
        for (int row = height - 1; row >= 0; row--) {
            displayLabel(imGui, Move.getRow(row), size / 2, size);
            imGui.sameLine();
            for (int col = 0; col < width; col++) {
                displayCell(imGui, size, row, col);
                imGui.sameLine();
            }
            displayLabel(imGui, Move.getRow(row), size / 2, size);
        }
        displayLabelRow(imGui, size);
        windowWidth = JImGuiGen.getWindowWidth();
        windowHeight = JImGuiGen.getWindowHeight();
        JImGuiGen.end();

        imGui.getStyle().setItemSpacingX(spacingX);
        imGui.getStyle().setItemSpacingY(spacingY);
        imGui.getStyle().setFramePaddingX(paddingX);
        imGui.getStyle().setFramePaddingY(paddingY);

        if (displayPromotionPopup) {
            imGui.openPopup("Promote");
        }

        if (displayResultPopup) {
            imGui.openPopup("Result");
        }

        if (imGui.beginPopup("Promote", JImWindowFlags.AlwaysAutoResize)) {
            for (PieceType pieceType : getSelectedPiece().getPromotionTypes()) {
                if (CellGraphics.display(imGui, getSelectedPiece().getSide(), pieceType, pieceType.getProperName(translation),
                        size, getColor(selectedPiece.x, selectedPiece.y).toSide().toColor(), -1)) {
                    getSelectedPiece().promoteTo(pieceType);
                    displayPromotionPopup = false;
                    JImGuiGen.closeCurrentPopup();
                    Move lastMove = moveLog.get(turn);
                    if (lastMove.getPromotion() != pieceType) {
                        lastMove.setPromotion(pieceType);
                        moveLog.subList(turn + 1, moveLog.size()).clear();
                    }
                    advanceTurn();
                    checkStatusConditions();
                    break;
                }
            }
            JImGuiGen.endPopup();
        }

        if (imGui.beginPopup("Result", JImWindowFlags.AlwaysAutoResize)) {
            imGui.text(status);
            if (imGui.button("OK")) {
                displayResultPopup = false;
                JImGuiGen.closeCurrentPopup();
            }
            JImGuiGen.endPopup();
        }
    }

    private void displayCell(JImGui imGui, float size, int row, int col) {
        if (CellGraphics.display(imGui, getPiece(row, col), getLabel(row, col), size, getColor(row, col), col * height + row)) {
            analyzeInput(row, col);
        }
    }

    private void displayLabel(@NotNull JImGui imGui, String label, float x, float y) {
        imGui.pushStyleColor(JImStyleColors.Button, Color.NONE.getColor());
        imGui.pushStyleColor(JImStyleColors.ButtonHovered, Color.NONE.getColor());
        imGui.pushStyleColor(JImStyleColors.ButtonActive, Color.NONE.getColor());
        imGui.button(label, x, y);
        JImGuiGen.popStyleColor(3);
    }

    private void displayLabelRow(JImGui imGui, float size) {
        displayLabel(imGui, "", size / 2, size / 2);
        imGui.sameLine();
        for (int col = 0; col < width; col++) {
            displayLabel(imGui, "" + Move.getColumn(col), size, size / 2);
            imGui.sameLine();
        }
        displayLabel(imGui, "", size / 2, size / 2);
    }

    public void displayLog(@NotNull JImGui imGui, int width, int height, int posX, int posY, int characterWidth) {
        imGui.setWindowSize("Turn Info", width, height);
        imGui.setWindowPos("Turn Info", posX, posY);
        imGui.begin("Turn Info", new NativeBool(), JImWindowFlags.NoMove | JImWindowFlags.NoTitleBar | JImWindowFlags.NoResize);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < turn; i++) {
            String move = moveLog.get(i).toNotation(translation);
            String paddedMove;
            if (i % 2 == 0) {
                move = String.format("%d. ", i / 2 + 1) + move;
            }
            paddedMove = move;
            if (i != 0) {
                if (i % 2 == 0) {
                    paddedMove = "   " + paddedMove;
                } else {
                    paddedMove = " " + paddedMove;
                }
            }
            if (sb.length() - sb.lastIndexOf("\n") + paddedMove.length() > characterWidth) {
                sb.append("\n");
                sb.append(move);
            } else {
                sb.append(paddedMove);
            }
        }
        String result = getResultString();
        if (result.length() > 0) {
            if (turn % 2 == 0) {
                result = "   " + result;
            } else {
                result = " " + result;
            }
            if (sb.length() - sb.lastIndexOf("\n") + result.length() > characterWidth) {
                sb.append("\n");
                sb.append(getResultString());
            } else {
                sb.append(result);
            }
        }
        imGui.text(sb.toString());
        JImGuiGen.end();
    }

    public float getWindowWidth() {
        return windowWidth;
    }

    public float getWindowHeight() {
        return windowHeight;
    }

    private void analyzeInput(int row, int col) {
        if (isSelected(row, col)) {
            deselectPiece();
        } else {
            if (selectedPieceIsValid()
                    && (Move.contains(availableMoves, row, col)
                    || Move.contains(availableCaptures, row, col))) {
                moveAndCheckStatusConditions(selectedPiece.x, selectedPiece.y, row, col);
            } else {
                selectPiece(row, col);
            }
        }
    }

    protected void move(int startRow, int startCol, int endRow, int endCol) {
        move(startRow, startCol, endRow, endCol, true);
    }

    protected void move(int startRow, int startCol, int endRow, int endCol, boolean moveDataNeeded) {
        Move move = new Move(getType(startRow, startCol), startRow, startCol, endRow, endCol);
        if (moveDataNeeded) {
            move.setData(this);
        }
        try {
            previousBoard = (Board) clone();
        } catch (CloneNotSupportedException ignore) {
            previousBoard = null;
        }
        Piece piece = getPiece(startRow, startCol);
        Point enPassantPoint = getEnPassantPoint(getTurnSide().getOpponent());
        Piece enPassantPiece = getPiece(enPassantPoint.x, enPassantPoint.y);
        if (enPassantPoint.x == endRow && enPassantPoint.y == endCol
                && (piece.getType() == PieceType.PAWN)
                && startCol != endCol) {
            placePiece(PieceFactory.cell(), endRow + MovementRules.getPawnMoveDirection(enPassantPiece.getSide()), endCol);
        }
        enPassantPoint = getEnPassantPoint(getTurnSide());
        if (enPassantPiece.getType() == PieceType.EMPTY) {
            placePiece(PieceFactory.cell(), enPassantPoint);
        }
        setEnPassantPoint(getTurnSide(), pieceNone);
        if (piece.getMovementType() instanceof DoublePawnType && endRow - startRow == 2 * MovementRules.getPawnMoveDirection(piece.getSide())) {
            setEnPassantPoint(getTurnSide(), new Point(endRow - MovementRules.getPawnMoveDirection(piece.getSide()), endCol));
            placePiece(PieceFactory.piece(PieceBaseType.NEUTRAL_PIECE, PieceType.EMPTY, piece.getSide()), getEnPassantPoint(getTurnSide()));
        }
        if (piece.getType() == PieceType.KING && endCol - startCol == 2) {
            getPiece(startRow, startCol + 3).move(this, startRow, startCol + 1);
        }
        if (piece.getType() == PieceType.KING && endCol - startCol == -2) {
            getPiece(startRow, startCol - 4).move(this, startRow, startCol - 1);
        }
        piece.move(this, endRow, endCol);
        selectedPiece = new Point(endRow, endCol);
        updatePieces(); //please do not remove this, even though advanceTurn() also calls it
        updateMove(move);
        if (getSelectedPiece().canBePromoted()) {
            displayPromotionPopup = true;
        } else {
            advanceTurn();
        }
    }

    private void moveAndCheckStatusConditions(int startRow, int startCol, int endRow, int endCol) {
        move(startRow, startCol, endRow, endCol);
        if (!displayPromotionPopup) {
            checkStatusConditions();
        }
    }

    private void updateMove(Move newMove) {
        if (moveLog.size() > turn) {
            if (moveLog.get(turn).hasDifferentMoveData(newMove)) {
                moveLog.subList(turn, moveLog.size()).clear();
                moveLog.add(newMove);
            }
        } else {
            moveLog.add(newMove);
        }
    }

    public boolean hasPromotion() {
        return displayPromotionPopup;
    }

    public boolean canRedo() {
        return moveLog.size() > turn;
    }

    public void redo() {
        if (canRedo()) {
            Move move = moveLog.get(turn);
            move(move.getStartRow(), move.getStartCol(), move.getEndRow(), move.getEndCol());
            if (getPiece(move.getEndRow(), move.getEndCol()).canBePromoted() && move.getPromotion() != PieceType.NONE) {
                getPiece(move.getEndRow(), move.getEndCol()).promoteTo(move.getPromotion());
                advanceTurn();
            }
            displayPromotionPopup = false;
            checkStatusConditions();
        }
    }

    private void checkStatusConditions() {
        updateMovablePieces();
        status = getStatusConditions(getTurnSide());
        if (isInCheck(getTurnSide())) {
            moveLog.get(turn - 1).setCheckFlag();
        }
        if (gameEnded) {
            moveLog.get(turn - 1).setGameEndFlag();
            displayResultPopup = true;
        }
    }

    @Nullable
    private String getStatusConditions(Side side) {
        if (getAllAvailableMoves(side).size() == 0 && getAllAvailableCaptures(side).size() == 0) {
            gameEnded = true;
            if (isInCheck(side)) {
                statusSide = side.getOpponent();
                statusPiece = PieceType.QUEEN;
                return String.format(translation.get("status_checkmate"), side.getOpponent().getProperName(translation));
            } else {
                statusSide = side.getOpponent();
                statusPiece = PieceType.KING;
                return String.format(translation.get("status_stalemate"),
                        side.getOpponent().getProperName(translation),
                        side.getProperName(translation));
            }
        } else {
            if (isInCheck(side)) {
                statusSide = side;
                statusPiece = PieceType.KING;
                return String.format(translation.get("status_check"), side.getProperName(translation));
            } else {
                statusSide = side;
                statusPiece = PieceType.PAWN;
                return null;
            }
        }
    }

    @NotNull
    private String getResultString() {
        if (gameEnded) {
            if (isInCheck(getTurnSide())) {
                if (getTurnSide() == Side.WHITE) {
                    return "0-1";
                } else {
                    return "1-0";
                }
            }
            return "1/2-1/2";
        }
        return "";
    }

    private Point getEnPassantPoint(Side side) {
        return side == Side.WHITE ? enPassantPointWhite : side == Side.BLACK ? enPassantPointBlack : pieceNone;
    }

    private void setEnPassantPoint(Side side, Point point) {
        if (side == Side.WHITE) {
            enPassantPointWhite = point;
        }
        if (side == Side.BLACK) {
            enPassantPointBlack = point;
        }
    }

    public String getStatusString() {
        return status;
    }

    public PieceType getStatusPiece() {
        return statusPiece;
    }

    public Side getStatusSide() {
        return statusSide;
    }

    public void placePiece(Piece piece, int row, int col) {
        try {
            board[row][col] = piece;
            getPiece(row, col).setPosition(row, col);
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
    }

    public void placePiece(Piece piece, @NotNull Point pos) {
        placePiece(piece, pos.x, pos.y);
    }

    private Color getColor(int row, int col) {
        if (nothingIsSelected()) {
            return (width - col + row) % 2 != 0 ? Color.WHITE : Color.BLACK;
        } else if (!selectedPieceIsValid() && movablePieces.contains(new Point(row, col))) {
            return (width - col + row) % 2 != 0 ? Color.MOVABLE_WHITE : Color.MOVABLE_BLACK;
        } else if (isSelected(row, col)) {
            return (width - col + row) % 2 != 0 ? Color.SELECTED_WHITE : Color.SELECTED_BLACK;
        } else if (Move.contains(availableMoves, row, col)) {
            return (width - col + row) % 2 != 0 ? Color.MOVE_WHITE : Color.MOVE_BLACK;
        } else if (Move.contains(availableCaptures, row, col)) {
            return (width - col + row) % 2 != 0 ? Color.CAPTURE_WHITE : Color.CAPTURE_BLACK;
        }
        return (width - col + row) % 2 != 0 ? Color.WHITE : Color.BLACK;
    }

    private void updatePieces() {
        pieces = new ArrayList<>();
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                if (!isFree(row, col)) {
                    getPiece(row, col).resetMoveCache();
                    pieces.add(new Point(row, col));
                }
            }
        }
    }

    private void updateMovablePieces() {
        movablePieces = new ArrayList<>();
        for (Point p : this.pieces) {
            if (getSide(p.x, p.y) == getTurnSide()) {
                ArrayList<Move> moves = new ArrayList<>();
                moves.addAll(getAvailableMoves(p.x, p.y));
                moves.addAll(getAvailableCaptures(p.x, p.y));
                if (!moves.isEmpty()) {
                    movablePieces.add(p);
                }
            }
        }
    }

    @NotNull
    private ArrayList<Move> getAllAvailableMovesWithoutSpecialRules(Side side) {
        ArrayList<Move> moves = new ArrayList<>();
        for (Point p : this.pieces) {
            if (getSide(p.x, p.y) == side) {
                moves.addAll(getPiece(p.x, p.y).getAvailableMovesWithoutSpecialRules(this));
            }
        }
        return moves;
    }

    @NotNull
    private ArrayList<Move> getAllAvailableCapturesWithoutSpecialRules(Side side) {
        ArrayList<Move> moves = new ArrayList<>();
        for (Point p : this.pieces) {
            if (getSide(p.x, p.y) == side) {
                moves.addAll(getPiece(p.x, p.y).getAvailableCapturesWithoutSpecialRules(this));
            }
        }
        return moves;
    }

    public ArrayList<Move> getAllAvailableMoves(Side side) {
        ArrayList<Move> moves = new ArrayList<>();
        for (Point p : this.pieces) {
            if (getSide(p.x, p.y) == side) {
                moves.addAll(getPiece(p.x, p.y).getAvailableMoves(this));
            }
        }
        return MovePriorities.topPriorityMoves(moves);
    }

    public ArrayList<Move> getAllAvailableCaptures(Side side) {
        ArrayList<Move> moves = new ArrayList<>();
        for (Point p : this.pieces) {
            if (getSide(p.x, p.y) == side) {
                moves.addAll(getPiece(p.x, p.y).getAvailableCaptures(this));
            }
        }
        return MovePriorities.topPriorityMoves(moves);
    }

    public boolean isFree(int row, int col) {
        return getPiece(row, col).getType() == PieceType.EMPTY;
    }

    public Side getSide(int row, int col) {
        return getPiece(row, col).getSide();
    }

    public PieceType getType(int row, int col) {
        return getPiece(row, col).getType();
    }

    public Piece getPiece(int row, int col) {
        try {
            return board[row][col];
        } catch (ArrayIndexOutOfBoundsException ignored) {
            return PieceFactory.none();
        }
    }

    private String getLabel(int row, int col) {
        return getPiece(row, col).getLabel(translation);
    }

    private Piece getSelectedPiece() {
        return getPiece(selectedPiece.x, selectedPiece.y);
    }

    protected void selectPiece(int row, int col) {
        getPiece(row, col).getType();
        selectedPiece = new Point(row, col);
        availableMoves = getAvailableMoves(row, col);
        availableCaptures = getAvailableCaptures(row, col);
    }

    private ArrayList<Move> getAvailableMoves(int row, int col) {
        if (getSide(row, col) == getTurnSide()) {
            ArrayList<Move> availableMoves = getPiece(row, col).getAvailableMoves(this);
            availableMoves = topPriorityMoves(availableMoves, row, col);
            return availableMoves;
        }
        return new ArrayList<>();
    }

    private ArrayList<Move> getAvailableCaptures(int row, int col) {
        if (getSide(row, col) == getTurnSide()) {
            ArrayList<Move> availableCaptures = getPiece(row, col).getAvailableCaptures(this);
            availableCaptures = topPriorityMoves(availableCaptures, row, col);
            return availableCaptures;
        }
        return new ArrayList<>();
    }

    private ArrayList<Move> topPriorityMoves(ArrayList<Move> moves, int row, int col) {
        ArrayList<Move> allAvailableMoves = getAllAvailableMoves(getSide(row, col));
        ArrayList<Move> allAvailableCaptures = getAllAvailableCaptures(getSide(row, col));
        int topPriority = Math.max(MovePriorities.getTopPriority(allAvailableMoves), MovePriorities.getTopPriority(allAvailableCaptures));
        if (MovePriorities.getTopPriority(moves) < topPriority) {
            moves = new ArrayList<>();
        }
        return MovePriorities.topPriorityMoves(moves);
    }

    private void deselectPiece() {
        selectedPiece = pieceNone;
        availableMoves = new ArrayList<>();
        availableCaptures = new ArrayList<>();
    }

    private boolean nothingIsSelected() {
        return selectedPiece.equals(pieceNone);
    }

    private boolean selectedPieceIsValid() {
        return movablePieces.contains(selectedPiece);
    }

    private boolean isSelected(int row, int col) {
        return selectedPiece.equals(new Point(row, col)) && selectedPieceIsValid();
    }

    public Side getTurnSide() {
        return turnIndicator;
    }

    private void advanceTurn() {
        deselectPiece();
        ++turn;
        turnIndicator = turnIndicator.getOpponent();
        updatePieces(); //this is needed in case a piece has been promoted, which changes its movepool
    }

    public Board getNextTurn(Move move) {
        Board nextTurn = new Board(board, translation, turn);
        if (move != null) {
            nextTurn.move(move.getStartRow(), move.getStartCol(), move.getEndRow(), move.getEndCol(), false);
        }
        return nextTurn;
    }

    public boolean isInCheck(@NotNull Side side) {
        updatePieces();
        boolean kingIsPresent = false;
        for (Point p : pieces) {
            if (getType(p.x, p.y) == PieceType.KING && getSide(p.x, p.y) == side) {
                kingIsPresent = true;
            }
        }
        if (!kingIsPresent) {
            return true;
        }
        ArrayList<Move> captures = getAllAvailableCapturesWithoutSpecialRules(side.getOpponent());
        for (Move capture : captures) {
            if (getType(capture.getEndRow(), capture.getEndCol()) == PieceType.KING) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    public ArrayList<Move> excludeMovesThatLeaveKingInCheck(Side side, @NotNull ArrayList<Move> moves) {
        ArrayList<Move> filteredMoves = new ArrayList<>();
        for (Move move : moves) {
            if (!getNextTurn(move).isInCheck(side)) {
                filteredMoves.add(move);
            }
        }
        return filteredMoves;
    }

    public Board getPreviousBoard() {
        return previousBoard;
    }

    protected Move getRandomMove() {
        ArrayList<Move> moves = new ArrayList<>();
        for (Point p : movablePieces) {
            moves.addAll(getAvailableMoves(p.x, p.y));
            moves.addAll(getAvailableCaptures(p.x, p.y));
        }
        if (moves.size() > 0) {
            return moves.get((int) (Math.random() * moves.size()));
        }
        return null;
    }

    public void makeRandomMove() {
        Move move = getRandomMove();
        if (move != null) {
            move.setData(this);
            updateMove(move);
            redo();
        }
    }
}
