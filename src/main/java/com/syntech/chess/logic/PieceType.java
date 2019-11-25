package com.syntech.chess.logic;

import com.syntech.chess.rules.MovementType;
import com.syntech.chess.rules.chess.*;
import com.syntech.chess.rules.neutral.ImmovableType;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum PieceType {
    EMPTY("Cell", "Empty Cell"),
    PAWN("Pawn"),
    DOUBLE_PAWN("Pawn"),
    KNIGHT("Knight"),
    BISHOP("Bishop"),
    ROOK("Rook"),
    QUEEN("Queen"),
    KING("King"),
    AMAZON("Amazon");

    private String name;
    private String properName;

    @Contract(pure = true)
    PieceType(String name) {
        this.name = name;
        this.properName = name;
    }

    @Contract(pure = true)
    PieceType(String name, String properName) {
        this.name = name;
        this.properName = properName;
    }

    @Contract(pure = true)
    public String getName() {
        return name;
    }

    @Contract(pure = true)
    public String getProperName() {
        return properName;
    }

    @Nullable
    public MovementType getMovementType(Side side) {
        switch (this) {
            case EMPTY:
                return new ImmovableType(this);
            case PAWN:
                return new PawnType(side);
            case DOUBLE_PAWN:
                return new DoublePawnType(side);
            case KNIGHT:
                return new KnightType();
            case BISHOP:
                return new BishopType();
            case ROOK:
                return new RookType();
            case QUEEN:
                return new QueenType();
            case KING:
                return new KingType(side);
            case AMAZON:
                return new AmazonType();
            default:
                return null;
        }
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public String toString() {
        return "PieceType{" +
                "name='" + name + '\'' +
                '}';
    }
}
