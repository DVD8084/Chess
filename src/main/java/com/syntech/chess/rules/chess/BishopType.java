package com.syntech.chess.rules.chess;

import com.syntech.chess.logic.Board;
import com.syntech.chess.logic.Move;
import com.syntech.chess.logic.PieceType;
import com.syntech.chess.logic.Side;
import com.syntech.chess.rules.MovementRules;
import com.syntech.chess.rules.MovementType;

import java.awt.*;
import java.util.ArrayList;

public class BishopType extends MovementType {

    @Override
    public ArrayList<Move> getAvailableMovesWithoutSpecialRules(Point position, Board board) {
        ArrayList<Move> moves = new ArrayList<>();
        MovementRules.addDiagonalMovement(position, board, moves);
        return moves;
    }

    @Override
    public ArrayList<Move> getAvailableThreatsOn(Point position, Board board, Side side) {
        ArrayList<Move> moves = new ArrayList<>();
        MovementRules.addDiagonalThreatening(position, board, side, moves);
        return moves;
    }

    @Override
    public PieceType getType() {
        return PieceType.BISHOP;
    }
}
