package com.github.relua.model;

public enum Opcode {
    MOVE,       /* A B R(A) := R(B) */
    LOADK,      /* A Bx R(A) := Kst(Bx) */
    LOADBOOL,   /* A B C R(A) := (Bool)B; if (C) pc++ */
    LOADNIL,    /* A B R(A) := ... := R(B) := nil */
    GETUPVAL,   /* A B R(A) := UpValue[B] */

    GETGLOBAL,  /* A Bx R(A) := Gbl[Kst(Bx)] */
    GETTABLE,   /* A B C R(A) := R(B)[RK(C)] */

    SETGLOBAL,  /* A Bx Gbl[Kst(Bx)] := R(A) */
    SETUPVAL,   /* A B UpValue[B] := R(A) */
    SETTABLE,   /* A B C R(A)[RK(B)] := RK(C) */

    NEWTABLE,   /* A B C R(A) := {} (size = B,C) */

    SELF,       /* A B C R(A+1) := R(B); R(A) := R(B)[RK(C)] */

    ADD,        /* A B C R(A) := RK(B) + RK(C) */
    SUB,        /* A B C R(A) := RK(B) - RK(C) */
    MUL,        /* A B C R(A) := RK(B) * RK(C) */
    DIV,        /* A B C R(A) := RK(B) / RK(C) */   
    MOD,        /* A B C R(A) := RK(B) % RK(C) */
    POW,        /* A B C R(A) := RK(B) ^ RK(C) */
    UNM,        /* A B R(A) := -R(B) */
    NOT,        /* A B R(A) := not R(B) */
    LEN,        /* A B R(A) := length of R(B) */

    CONCAT,     /* A B C R(A) := R(B).. ... ..R(C) */   

    JMP,        /* sBx pc+=sBx */

    EQ,        /* A B C if ((RK(B) == RK(C)) ~= A) then pc++ */
    LT,        /* A B C if ((RK(B) < RK(C)) ~= A) then pc++ */
    LE,        /* A B C if ((RK(B) <= RK(C)) ~= A) then pc++ */

    TEST,      /* A C if not (R(A) <=> C) then pc++ */
    TESTSET,   /* A B C if (R(B) <=> C) then R(A) := R(B) else pc++ */

    CALL,      /* A B C R(A), ... ,R(A+C-2) := R(A)(R(A+1), ... ,R(A+B-1)) */
    TAILCALL,  /* A B C return R(A)(R(A+1), ... ,R(A+B-1)) */
    RETURN,    /* A B return R(A), ... ,R(A+B-2) (see note) */

    FORLOOP,   /*
                 * A sBx R(A)+=R(A+2);
                 * if R(A) <?= R(A+1) then { pc+=sBx; R(A+3)=R(A) }
                 */
    FORPREP,    /* A sBx R(A)-=R(A+2); pc+=sBx */

    TFORLOOP,  /*
                  * A C R(A+3), ... ,R(A+2+C) := R(A)(R(A+1), R(A+2));
                  * if R(A+3) ~= nil then R(A+2)=R(A+3) else pc++
                  */
    SETLIST,   /* A B C R(A)[(C-1)*FPF+i] := R(A+i), 1 <= i <= B */

    CLOSE,     /* A close all variables in the stack up to (>=) R(A) */
    CLOSURE,   /* A Bx R(A) := closure(KPROTO[Bx], R(A), ... ,R(A+n)) */

    VARARG,    /* A B R(A), R(A+1), ..., R(A+B-1) = vararg */
    UNKNOWN
}
