package com.syntech.chess.rules.chess;

import com.syntech.chess.logic.Board;
import com.syntech.chess.logic.Move;
import com.syntech.chess.logic.PieceType;
import com.syntech.chess.logic.Side;
import com.syntech.chess.rules.MovementRules;
import com.syntech.chess.rules.MovementType;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;

public class KnightType extends MovementType {

    @Override
    public ArrayList<Move> getAvailableMovesWithoutSpecialRules(@NotNull Point position, @NotNull Board board) {
        ArrayList<Move> moves = new ArrayList<>();
        MovementRules.addLeapingMovement(position, board, 1, 2, moves);
        return moves;
    }

    @Override
    public ArrayList<Move> getAvailableThreatsOn(Point position, Board board, Side side) {
        ArrayList<Move> moves = new ArrayList<>();
        MovementRules.addLeapingThreatening(position, board, 1, 2, side, moves);
        return moves;
    }

    @Override
    public PieceType getType() {
        return PieceType.KNIGHT;
    }
}
