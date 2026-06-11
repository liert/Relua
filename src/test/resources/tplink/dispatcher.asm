; Function:        0
; Defined at line: 0
; #Upvalues:       0
; #Parameters:     0
; Is_vararg:       2
; Max Stack Size:  24

    0 [-]: GETGLOBAL R0 K0        ; R0 := require
    1 [-]: LOADK     R1 K1        ; R1 := "nixio.fs"
    2 [-]: CALL      R0 2 2       ; R0 := R0(R1)
    3 [-]: GETGLOBAL R1 K0        ; R1 := require
    4 [-]: LOADK     R2 K2        ; R2 := "luci.sys"
    5 [-]: CALL      R1 2 2       ; R1 := R1(R2)
    6 [-]: GETGLOBAL R2 K0        ; R2 := require
    7 [-]: LOADK     R3 K3        ; R3 := "luci.util"
    8 [-]: CALL      R2 2 2       ; R2 := R2(R3)
    9 [-]: GETGLOBAL R3 K0        ; R3 := require
   10 [-]: LOADK     R4 K4        ; R4 := "luci.http"
   11 [-]: CALL      R3 2 2       ; R3 := R3(R4)
   12 [-]: GETGLOBAL R4 K0        ; R4 := require
   13 [-]: LOADK     R5 K5        ; R5 := "luci.forbidden"
   14 [-]: CALL      R4 2 2       ; R4 := R4(R5)
   15 [-]: GETGLOBAL R5 K0        ; R5 := require
   16 [-]: LOADK     R6 K6        ; R6 := "nixio"
   17 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   18 [-]: GETGLOBAL R6 K0        ; R6 := require
   19 [-]: LOADK     R7 K7        ; R7 := "nixio.util"
   20 [-]: CALL      R6 2 1       ;  := R6(R7)
   21 [-]: GETGLOBAL R6 K8        ; R6 := module
   22 [-]: LOADK     R7 K9        ; R7 := "luci.dispatcher"
   23 [-]: GETGLOBAL R8 K10       ; R8 := package
   24 [-]: GETTABLE  R8 R8 K11    ; R8 := R8["seeall"]
   25 [-]: CALL      R6 3 1       ;  := R6(R7 to R8)
   26 [-]: GETTABLE  R6 R2 K13    ; R6 := R2["threadlocal"]
   27 [-]: CALL      R6 1 2       ; R6 := R6()
   28 [-]: SETGLOBAL R6 K12       ; context := R6
   29 [-]: GETGLOBAL R6 K0        ; R6 := require
   30 [-]: LOADK     R7 K15       ; R7 := "luci.model.uci"
   31 [-]: CALL      R6 2 2       ; R6 := R6(R7)
   32 [-]: SETGLOBAL R6 K14       ; uci := R6
   33 [-]: GETGLOBAL R6 K0        ; R6 := require
   34 [-]: LOADK     R7 K17       ; R7 := "luci.i18n"
   35 [-]: CALL      R6 2 2       ; R6 := R6(R7)
   36 [-]: SETGLOBAL R6 K16       ; i18n := R6
   37 [-]: GETGLOBAL R6 K18       ; R6 := _M
   38 [-]: SETTABLE  R6 K19 R0    ; R6["fs"] := R0
   39 [-]: LOADNIL   R6 R7        ; R6 to R7 := nil
   40 [-]: CLOSURE   R8 0         ; R8 := closure(Function #0_0)
   41 [-]: MOVE      R0 R3        ; R0 := R3
   42 [-]: SETGLOBAL R8 K20       ; build_url := R8
   43 [-]: CLOSURE   R8 1         ; R8 := closure(Function #0_1)
   44 [-]: SETGLOBAL R8 K21       ; node_visible := R8
   45 [-]: CLOSURE   R8 2         ; R8 := closure(Function #0_2)
   46 [-]: MOVE      R0 R2        ; R0 := R2
   47 [-]: SETGLOBAL R8 K22       ; node_childs := R8
   48 [-]: CLOSURE   R8 3         ; R8 := closure(Function #0_3)
   49 [-]: MOVE      R0 R3        ; R0 := R3
   50 [-]: MOVE      R0 R2        ; R0 := R2
   51 [-]: SETGLOBAL R8 K23       ; error404 := R8
   52 [-]: CLOSURE   R8 4         ; R8 := closure(Function #0_4)
   53 [-]: MOVE      R0 R2        ; R0 := R2
   54 [-]: MOVE      R0 R3        ; R0 := R3
   55 [-]: SETGLOBAL R8 K24       ; error500 := R8
   56 [-]: CLOSURE   R8 5         ; R8 := closure(Function #0_5)
   57 [-]: MOVE      R0 R3        ; R0 := R3
   58 [-]: MOVE      R0 R2        ; R0 := R2
   59 [-]: SETGLOBAL R8 K25       ; httpdispatch := R8
   60 [-]: CLOSURE   R8 6         ; R8 := closure(Function #0_6)
   61 [-]: MOVE      R0 R3        ; R0 := R3
   62 [-]: CLOSURE   R9 7         ; R9 := closure(Function #0_7)
   63 [-]: MOVE      R0 R3        ; R0 := R3
   64 [-]: SETGLOBAL R9 K26       ; test_post_security := R9
   65 [-]: CLOSURE   R9 8         ; R9 := closure(Function #0_8)
   66 [-]: MOVE      R0 R2        ; R0 := R2
   67 [-]: CLOSURE   R10 9        ; R10 := closure(Function #0_9)
   68 [-]: MOVE      R0 R2        ; R0 := R2
   69 [-]: CLOSURE   R11 10       ; R11 := closure(Function #0_10)
   70 [-]: MOVE      R0 R2        ; R0 := R2
   71 [-]: CLOSURE   R12 11       ; R12 := closure(Function #0_11)
   72 [-]: MOVE      R0 R2        ; R0 := R2
   73 [-]: CLOSURE   R13 12       ; R13 := closure(Function #0_12)
   74 [-]: MOVE      R0 R1        ; R0 := R1
   75 [-]: CLOSURE   R14 13       ; R14 := closure(Function #0_13)
   76 [-]: MOVE      R0 R2        ; R0 := R2
   77 [-]: CLOSURE   R15 14       ; R15 := closure(Function #0_14)
   78 [-]: MOVE      R0 R2        ; R0 := R2
   79 [-]: MOVE      R0 R3        ; R0 := R3
   80 [-]: MOVE      R0 R1        ; R0 := R1
   81 [-]: CLOSURE   R16 15       ; R16 := closure(Function #0_15)
   82 [-]: MOVE      R0 R3        ; R0 := R3
   83 [-]: MOVE      R0 R1        ; R0 := R1
   84 [-]: MOVE      R0 R2        ; R0 := R2
   85 [-]: CLOSURE   R17 16       ; R17 := closure(Function #0_16)
   86 [-]: MOVE      R0 R2        ; R0 := R2
   87 [-]: MOVE      R0 R14       ; R0 := R14
   88 [-]: MOVE      R0 R16       ; R0 := R16
   89 [-]: MOVE      R0 R1        ; R0 := R1
   90 [-]: MOVE      R0 R11       ; R0 := R11
   91 [-]: MOVE      R0 R9        ; R0 := R9
   92 [-]: MOVE      R0 R10       ; R0 := R10
   93 [-]: CLOSURE   R18 17       ; R18 := closure(Function #0_17)
   94 [-]: SETGLOBAL R18 K27      ; host_redirect_check := R18
   95 [-]: CLOSURE   R18 18       ; R18 := closure(Function #0_18)
   96 [-]: SETGLOBAL R18 K28      ; tls_redirect_check := R18
   97 [-]: CLOSURE   R18 19       ; R18 := closure(Function #0_19)
   98 [-]: MOVE      R0 R3        ; R0 := R3
   99 [-]: MOVE      R0 R2        ; R0 := R2
  100 [-]: MOVE      R0 R0        ; R0 := R0
  101 [-]: MOVE      R0 R1        ; R0 := R1
  102 [-]: MOVE      R0 R9        ; R0 := R9
  103 [-]: MOVE      R0 R15       ; R0 := R15
  104 [-]: MOVE      R0 R12       ; R0 := R12
  105 [-]: MOVE      R0 R17       ; R0 := R17
  106 [-]: MOVE      R0 R13       ; R0 := R13
  107 [-]: MOVE      R0 R14       ; R0 := R14
  108 [-]: MOVE      R0 R4        ; R0 := R4
  109 [-]: MOVE      R0 R8        ; R0 := R8
  110 [-]: SETGLOBAL R18 K29      ; dispatch := R18
  111 [-]: CLOSURE   R18 20       ; R18 := closure(Function #0_20)
  112 [-]: MOVE      R0 R2        ; R0 := R2
  113 [-]: MOVE      R0 R0        ; R0 := R0
  114 [-]: MOVE      R0 R1        ; R0 := R1
  115 [-]: MOVE      R0 R6        ; R0 := R6
  116 [-]: MOVE      R0 R5        ; R0 := R5
  117 [-]: SETGLOBAL R18 K30      ; createindex := R18
  118 [-]: CLOSURE   R18 21       ; R18 := closure(Function #0_21)
  119 [-]: MOVE      R0 R6        ; R0 := R6
  120 [-]: MOVE      R0 R2        ; R0 := R2
  121 [-]: SETGLOBAL R18 K31      ; createtree := R18
  122 [-]: CLOSURE   R18 22       ; R18 := closure(Function #0_22)
  123 [-]: SETGLOBAL R18 K32      ; modifier := R18
  124 [-]: CLOSURE   R18 23       ; R18 := closure(Function #0_23)
  125 [-]: SETGLOBAL R18 K33      ; assign := R18
  126 [-]: CLOSURE   R18 24       ; R18 := closure(Function #0_24)
  127 [-]: SETGLOBAL R18 K34      ; entry := R18
  128 [-]: CLOSURE   R18 25       ; R18 := closure(Function #0_25)
  129 [-]: SETGLOBAL R18 K35      ; get := R18
  130 [-]: CLOSURE   R18 26       ; R18 := closure(Function #0_26)
  131 [-]: SETGLOBAL R18 K36      ; node := R18
  132 [-]: CLOSURE   R18 27       ; R18 := closure(Function #0_27)
  133 [-]: SETGLOBAL R18 K37      ; _create_node := R18
  134 [-]: CLOSURE   R18 28       ; R18 := closure(Function #0_28)
  135 [-]: SETGLOBAL R18 K38      ; _firstchild := R18
  136 [-]: CLOSURE   R18 29       ; R18 := closure(Function #0_29)
  137 [-]: SETGLOBAL R18 K39      ; firstchild := R18
  138 [-]: CLOSURE   R18 30       ; R18 := closure(Function #0_30)
  139 [-]: SETGLOBAL R18 K40      ; alias := R18
  140 [-]: CLOSURE   R18 31       ; R18 := closure(Function #0_31)
  141 [-]: MOVE      R0 R2        ; R0 := R2
  142 [-]: SETGLOBAL R18 K41      ; rewrite := R18
  143 [-]: CLOSURE   R18 32       ; R18 := closure(Function #0_32)
  144 [-]: CLOSURE   R19 33       ; R19 := closure(Function #0_33)
  145 [-]: MOVE      R0 R18       ; R0 := R18
  146 [-]: SETGLOBAL R19 K42      ; call := R19
  147 [-]: CLOSURE   R19 34       ; R19 := closure(Function #0_34)
  148 [-]: MOVE      R0 R18       ; R0 := R18
  149 [-]: SETGLOBAL R19 K43      ; post_on := R19
  150 [-]: CLOSURE   R19 35       ; R19 := closure(Function #0_35)
  151 [-]: SETGLOBAL R19 K44      ; post := R19
  152 [-]: CLOSURE   R19 36       ; R19 := closure(Function #0_36)
  153 [-]: CLOSURE   R20 37       ; R20 := closure(Function #0_37)
  154 [-]: MOVE      R0 R19       ; R0 := R19
  155 [-]: SETGLOBAL R20 K45      ; template := R20
  156 [-]: CLOSURE   R20 38       ; R20 := closure(Function #0_38)
  157 [-]: CLOSURE   R21 39       ; R21 := closure(Function #0_39)
  158 [-]: MOVE      R0 R20       ; R0 := R20
  159 [-]: SETGLOBAL R21 K46      ; cbi := R21
  160 [-]: CLOSURE   R21 40       ; R21 := closure(Function #0_40)
  161 [-]: CLOSURE   R22 41       ; R22 := closure(Function #0_41)
  162 [-]: MOVE      R0 R21       ; R0 := R21
  163 [-]: SETGLOBAL R22 K47      ; arcombine := R22
  164 [-]: CLOSURE   R22 42       ; R22 := closure(Function #0_42)
  165 [-]: CLOSURE   R23 43       ; R23 := closure(Function #0_43)
  166 [-]: MOVE      R0 R22       ; R0 := R22
  167 [-]: SETGLOBAL R23 K48      ; form := R23
  168 [-]: GETGLOBAL R23 K16      ; R23 := i18n
  169 [-]: GETTABLE  R23 R23 K49  ; R23 := R23["translate"]
  170 [-]: SETGLOBAL R23 K49      ; translate := R23
  171 [-]: CLOSURE   R23 44       ; R23 := closure(Function #0_44)
  172 [-]: SETGLOBAL R23 K50      ; _ := R23
  173 [-]: RETURN    R0 1         ; return 


; Function:        0_0
; Defined at line: 25
; #Upvalues:       1
; #Parameters:     0
; Is_vararg:       3
; Max Stack Size:  12

    0 [-]: NEWTABLE  R1 0 0       ; R1 := {} (size = 0,0)
    1 [-]: VARARG    R2 0         ; R2 to top := ...
    2 [-]: SETLIST   R1 0 1       ; R1[0] to R1[top] := R2 to top ; R(a)[(c-1)*FPF+i] := R(a+i), 1 <= i <= b, a=1, b=0, c=1, FPF=50
    3 [-]: NEWTABLE  R2 1 0       ; R2 := {} (size = 1,0)
    4 [-]: GETUPVAL  R3 U0        ; R3 := U0
    5 [-]: GETTABLE  R3 R3 K0     ; R3 := R3["getenv"]
    6 [-]: LOADK     R4 K1        ; R4 := "SCRIPT_NAME"
    7 [-]: CALL      R3 2 2       ; R3 := R3(R4)
    8 [-]: TEST      R3 1         ; if not R3 then goto 10 else goto 11
    9 [-]: JMP       1            ; PC += 1 (goto 11)
   10 [-]: LOADK     R3 K2        ; R3 := ""
   11 [-]: SETLIST   R2 1 1       ; R2[0] := R3 ; R(a)[(c-1)*FPF+i] := R(a+i), 1 <= i <= b, a=2, b=1, c=1, FPF=50
   12 [-]: LOADNIL   R3 R3        ; R3 := nil
   13 [-]: GETGLOBAL R4 K3        ; R4 := ipairs
   14 [-]: MOVE      R5 R1        ; R5 := R1
   15 [-]: CALL      R4 2 4       ; R4 to R6 := R4(R5)
   16 [-]: JMP       11           ; PC += 11 (goto 28)
   17 [-]: SELF      R9 R8 K4     ; R10 := R8; R9 := R8["match"]
   18 [-]: LOADK     R11 K5       ; R11 := "^[a-zA-Z0-9_%-%.%%/,;]+$"
   19 [-]: CALL      R9 3 2       ; R9 := R9(R10 to R11)
   20 [-]: TEST      R9 0         ; if R9 then goto 22 else goto 28
   21 [-]: JMP       6            ; PC += 6 (goto 28)
   22 [-]: LEN       R9 R2        ; R9 := #R2
   23 [-]: ADD       R9 R9 K6     ; R9 := R9 + Unknown_Type_Error
   24 [-]: SETTABLE  R2 R9 K7     ; R2[R9] := "/"
   25 [-]: LEN       R9 R2        ; R9 := #R2
   26 [-]: ADD       R9 R9 K6     ; R9 := R9 + Unknown_Type_Error
   27 [-]: SETTABLE  R2 R9 R8     ; R2[R9] := R8
   28 [-]: TFORLOOP  R4 2         ; R7 to R8 := R4(R5,R6); if R7 ~= nil then R6 := R7 else goto 30
   29 [-]: JMP       -13          ; PC += -13 (goto 17)
   30 [-]: LEN       R4 R1        ; R4 := #R1
   31 [-]: EQ        0 R4 K8      ; if R4 == Unknown_Type_Error then goto 33 else goto 36
   32 [-]: JMP       3            ; PC += 3 (goto 36)
   33 [-]: LEN       R4 R2        ; R4 := #R2
   34 [-]: ADD       R4 R4 K6     ; R4 := R4 + Unknown_Type_Error
   35 [-]: SETTABLE  R2 R4 K7     ; R2[R4] := "/"
   36 [-]: GETGLOBAL R4 K9        ; R4 := table
   37 [-]: GETTABLE  R4 R4 K10    ; R4 := R4["concat"]
   38 [-]: MOVE      R5 R2        ; R5 := R2
   39 [-]: LOADK     R6 K2        ; R6 := ""
   40 [-]: TAILCALL  R4 3 0       ; R4 to top := R4(R5 to R6)
   41 [-]: RETURN    R4 0         ; return R4 to top
   42 [-]: RETURN    R0 1         ; return 


; Function:        0_1
; Defined at line: 44
; #Upvalues:       0
; #Parameters:     1
; Is_vararg:       0
; Max Stack Size:  3

    0 [-]: TEST      R0 0         ; if R0 then goto 2 else goto 38
    1 [-]: JMP       36           ; PC += 36 (goto 38)
    2 [-]: GETTABLE  R1 R0 K0     ; R1 := R0["title"]
    3 [-]: TEST      R1 0         ; if R1 then goto 5 else goto 35
    4 [-]: JMP       30           ; PC += 30 (goto 35)
    5 [-]: GETTABLE  R1 R0 K0     ; R1 := R0["title"]
    6 [-]: LEN       R1 R1        ; R1 := #R1
    7 [-]: EQ        1 R1 K1      ; if R1 ~= Unknown_Type_Error then goto 9 else goto 35
    8 [-]: JMP       26           ; PC += 26 (goto 35)
    9 [-]: GETTABLE  R1 R0 K2     ; R1 := R0["target"]
   10 [-]: TEST      R1 0         ; if R1 then goto 12 else goto 35
   11 [-]: JMP       23           ; PC += 23 (goto 35)
   12 [-]: GETTABLE  R1 R0 K3     ; R1 := R0["hidden"]
   13 [-]: EQ        1 R1 K4      ; if R1 ~= true then goto 15 else goto 35
   14 [-]: JMP       20           ; PC += 20 (goto 35)
   15 [-]: GETGLOBAL R1 K5        ; R1 := type
   16 [-]: GETTABLE  R2 R0 K2     ; R2 := R0["target"]
   17 [-]: CALL      R1 2 2       ; R1 := R1(R2)
   18 [-]: EQ        0 R1 K6      ; if R1 == "table" then goto 20 else goto 36
   19 [-]: JMP       16           ; PC += 16 (goto 36)
   20 [-]: GETTABLE  R1 R0 K2     ; R1 := R0["target"]
   21 [-]: GETTABLE  R1 R1 K5     ; R1 := R1["type"]
   22 [-]: EQ        0 R1 K7      ; if R1 == "firstchild" then goto 24 else goto 36
   23 [-]: JMP       12           ; PC += 12 (goto 36)
   24 [-]: GETGLOBAL R1 K5        ; R1 := type
   25 [-]: GETTABLE  R2 R0 K8     ; R2 := R0["nodes"]
   26 [-]: CALL      R1 2 2       ; R1 := R1(R2)
   27 [-]: EQ        0 R1 K6      ; if R1 == "table" then goto 29 else goto 35
   28 [-]: JMP       6            ; PC += 6 (goto 35)
   29 [-]: GETGLOBAL R1 K9        ; R1 := next
   30 [-]: GETTABLE  R2 R0 K8     ; R2 := R0["nodes"]
   31 [-]: CALL      R1 2 2       ; R1 := R1(R2)
   32 [-]: NOT       R1 R1        ; R1 := not R1
   33 [-]: NOT       R1 R1        ; R1 := not R1
   34 [-]: JMP       2            ; PC += 2 (goto 37)
   35 [-]: LOADBOOL  R1 0 1       ; R1 := false; goto 37
   36 [-]: LOADBOOL  R1 1 0       ; R1 := true
   37 [-]: RETURN    R1 2         ; return R1
   38 [-]: LOADBOOL  R1 0 0       ; R1 := false
   39 [-]: RETURN    R1 2         ; return R1
   40 [-]: RETURN    R0 1         ; return 


; Function:        0_2
; Defined at line: 56
; #Upvalues:       1
; #Parameters:     1
; Is_vararg:       0
; Max Stack Size:  11

    0 [-]: NEWTABLE  R1 0 0       ; R1 := {} (size = 0,0)
    1 [-]: TEST      R0 0         ; if R0 then goto 3 else goto 21
    2 [-]: JMP       18           ; PC += 18 (goto 21)
    3 [-]: LOADNIL   R2 R3        ; R2 to R3 := nil
    4 [-]: GETUPVAL  R4 U0        ; R4 := U0
    5 [-]: GETTABLE  R4 R4 K0     ; R4 := R4["spairs"]
    6 [-]: GETTABLE  R5 R0 K1     ; R5 := R0["nodes"]
    7 [-]: CLOSURE   R6 0         ; R6 := closure(Function #0_2_0)
    8 [-]: MOVE      R0 R0        ; R0 := R0
    9 [-]: CALL      R4 3 4       ; R4 to R6 := R4(R5 to R6)
   10 [-]: JMP       8            ; PC += 8 (goto 19)
   11 [-]: GETGLOBAL R9 K2        ; R9 := node_visible
   12 [-]: MOVE      R10 R8       ; R10 := R8
   13 [-]: CALL      R9 2 2       ; R9 := R9(R10)
   14 [-]: TEST      R9 0         ; if R9 then goto 16 else goto 19
   15 [-]: JMP       3            ; PC += 3 (goto 19)
   16 [-]: LEN       R9 R1        ; R9 := #R1
   17 [-]: ADD       R9 R9 K3     ; R9 := R9 + Unknown_Type_Error
   18 [-]: SETTABLE  R1 R9 R7     ; R1[R9] := R7
   19 [-]: TFORLOOP  R4 2         ; R7 to R8 := R4(R5,R6); if R7 ~= nil then R6 := R7 else goto 21
   20 [-]: JMP       -10          ; PC += -10 (goto 11)
   21 [-]: RETURN    R1 2         ; return R1
   22 [-]: RETURN    R0 1         ; return 


; Function:        0_2_0
; Defined at line: 61
; #Upvalues:       1
; #Parameters:     2
; Is_vararg:       0
; Max Stack Size:  4

    0 [-]: GETUPVAL  R2 U0        ; R2 := U0
    1 [-]: GETTABLE  R2 R2 K0     ; R2 := R2["nodes"]
    2 [-]: GETTABLE  R2 R2 R0     ; R2 := R2[R0]
    3 [-]: GETTABLE  R2 R2 K1     ; R2 := R2["order"]
    4 [-]: TEST      R2 1         ; if not R2 then goto 6 else goto 7
    5 [-]: JMP       1            ; PC += 1 (goto 7)
    6 [-]: LOADK     R2 K2        ; R2 := Unknown_Type_Error
    7 [-]: GETUPVAL  R3 U0        ; R3 := U0
    8 [-]: GETTABLE  R3 R3 K0     ; R3 := R3["nodes"]
    9 [-]: GETTABLE  R3 R3 R1     ; R3 := R3[R1]
   10 [-]: GETTABLE  R3 R3 K1     ; R3 := R3["order"]
   11 [-]: TEST      R3 1         ; if not R3 then goto 13 else goto 14
   12 [-]: JMP       1            ; PC += 1 (goto 14)
   13 [-]: LOADK     R3 K2        ; R3 := Unknown_Type_Error
   14 [-]: LT        1 R2 R3      ; if R2 >= R3 then goto 16 else goto 17
   15 [-]: JMP       1            ; PC += 1 (goto 17)
   16 [-]: LOADBOOL  R2 0 1       ; R2 := false; goto 18
   17 [-]: LOADBOOL  R2 1 0       ; R2 := true
   18 [-]: RETURN    R2 2         ; return R2
   19 [-]: RETURN    R0 1         ; return 


; Function:        0_3
; Defined at line: 75
; #Upvalues:       2
; #Parameters:     1
; Is_vararg:       0
; Max Stack Size:  4

    0 [-]: GETUPVAL  R1 U0        ; R1 := U0
    1 [-]: GETTABLE  R1 R1 K0     ; R1 := R1["status"]
    2 [-]: LOADK     R2 K1        ; R2 := Unknown_Type_Error
    3 [-]: LOADK     R3 K2        ; R3 := "Not Found"
    4 [-]: CALL      R1 3 1       ;  := R1(R2 to R3)
    5 [-]: TEST      R0 1         ; if not R0 then goto 7 else goto 8
    6 [-]: JMP       1            ; PC += 1 (goto 8)
    7 [-]: LOADK     R0 K2        ; R0 := "Not Found"
    8 [-]: GETGLOBAL R1 K3        ; R1 := require
    9 [-]: LOADK     R2 K4        ; R2 := "luci.template"
   10 [-]: CALL      R1 2 1       ;  := R1(R2)
   11 [-]: GETUPVAL  R1 U1        ; R1 := U1
   12 [-]: GETTABLE  R1 R1 K5     ; R1 := R1["copcall"]
   13 [-]: GETGLOBAL R2 K6        ; R2 := luci
   14 [-]: GETTABLE  R2 R2 K7     ; R2 := R2["template"]
   15 [-]: GETTABLE  R2 R2 K8     ; R2 := R2["render"]
   16 [-]: LOADK     R3 K9        ; R3 := "error404"
   17 [-]: CALL      R1 3 2       ; R1 := R1(R2 to R3)
   18 [-]: TEST      R1 1         ; if not R1 then goto 20 else goto 28
   19 [-]: JMP       8            ; PC += 8 (goto 28)
   20 [-]: GETUPVAL  R1 U0        ; R1 := U0
   21 [-]: GETTABLE  R1 R1 K10    ; R1 := R1["prepare_content"]
   22 [-]: LOADK     R2 K11       ; R2 := "text/plain"
   23 [-]: CALL      R1 2 1       ;  := R1(R2)
   24 [-]: GETUPVAL  R1 U0        ; R1 := U0
   25 [-]: GETTABLE  R1 R1 K12    ; R1 := R1["write"]
   26 [-]: MOVE      R2 R0        ; R2 := R0
   27 [-]: CALL      R1 2 1       ;  := R1(R2)
   28 [-]: LOADBOOL  R1 0 0       ; R1 := false
   29 [-]: RETURN    R1 2         ; return R1
   30 [-]: RETURN    R0 1         ; return 


; Function:        0_4
; Defined at line: 87
; #Upvalues:       2
; #Parameters:     1
; Is_vararg:       0
; Max Stack Size:  5

    0 [-]: GETUPVAL  R1 U0        ; R1 := U0
    1 [-]: GETTABLE  R1 R1 K0     ; R1 := R1["perror"]
    2 [-]: MOVE      R2 R0        ; R2 := R0
    3 [-]: CALL      R1 2 1       ;  := R1(R2)
    4 [-]: GETGLOBAL R1 K1        ; R1 := context
    5 [-]: GETTABLE  R1 R1 K2     ; R1 := R1["template_header_sent"]
    6 [-]: TEST      R1 1         ; if not R1 then goto 8 else goto 22
    7 [-]: JMP       14           ; PC += 14 (goto 22)
    8 [-]: GETUPVAL  R1 U1        ; R1 := U1
    9 [-]: GETTABLE  R1 R1 K3     ; R1 := R1["status"]
   10 [-]: LOADK     R2 K4        ; R2 := Unknown_Type_Error
   11 [-]: LOADK     R3 K5        ; R3 := "Internal Server Error"
   12 [-]: CALL      R1 3 1       ;  := R1(R2 to R3)
   13 [-]: GETUPVAL  R1 U1        ; R1 := U1
   14 [-]: GETTABLE  R1 R1 K6     ; R1 := R1["prepare_content"]
   15 [-]: LOADK     R2 K7        ; R2 := "text/plain"
   16 [-]: CALL      R1 2 1       ;  := R1(R2)
   17 [-]: GETUPVAL  R1 U1        ; R1 := U1
   18 [-]: GETTABLE  R1 R1 K8     ; R1 := R1["write"]
   19 [-]: MOVE      R2 R0        ; R2 := R0
   20 [-]: CALL      R1 2 1       ;  := R1(R2)
   21 [-]: JMP       22           ; PC += 22 (goto 44)
   22 [-]: GETGLOBAL R1 K9        ; R1 := require
   23 [-]: LOADK     R2 K10       ; R2 := "luci.template"
   24 [-]: CALL      R1 2 1       ;  := R1(R2)
   25 [-]: GETUPVAL  R1 U0        ; R1 := U0
   26 [-]: GETTABLE  R1 R1 K11    ; R1 := R1["copcall"]
   27 [-]: GETGLOBAL R2 K12       ; R2 := luci
   28 [-]: GETTABLE  R2 R2 K13    ; R2 := R2["template"]
   29 [-]: GETTABLE  R2 R2 K14    ; R2 := R2["render"]
   30 [-]: LOADK     R3 K15       ; R3 := "error500"
   31 [-]: NEWTABLE  R4 0 1       ; R4 := {} (size = 0,1)
   32 [-]: SETTABLE  R4 K16 R0    ; R4["message"] := R0
   33 [-]: CALL      R1 4 2       ; R1 := R1(R2 to R4)
   34 [-]: TEST      R1 1         ; if not R1 then goto 36 else goto 44
   35 [-]: JMP       8            ; PC += 8 (goto 44)
   36 [-]: GETUPVAL  R1 U1        ; R1 := U1
   37 [-]: GETTABLE  R1 R1 K6     ; R1 := R1["prepare_content"]
   38 [-]: LOADK     R2 K7        ; R2 := "text/plain"
   39 [-]: CALL      R1 2 1       ;  := R1(R2)
   40 [-]: GETUPVAL  R1 U1        ; R1 := U1
   41 [-]: GETTABLE  R1 R1 K8     ; R1 := R1["write"]
   42 [-]: MOVE      R2 R0        ; R2 := R0
   43 [-]: CALL      R1 2 1       ;  := R1(R2)
   44 [-]: LOADBOOL  R1 0 0       ; R1 := false
   45 [-]: RETURN    R1 2         ; return R1
   46 [-]: RETURN    R0 1         ; return 


; Function:        0_5
; Defined at line: 103
; #Upvalues:       2
; #Parameters:     2
; Is_vararg:       0
; Max Stack Size:  10

    0 [-]: GETUPVAL  R2 U0        ; R2 := U0
    1 [-]: GETTABLE  R2 R2 K0     ; R2 := R2["context"]
    2 [-]: SETTABLE  R2 K1 R0     ; R2["request"] := R0
    3 [-]: NEWTABLE  R2 0 0       ; R2 := {} (size = 0,0)
    4 [-]: GETGLOBAL R3 K0        ; R3 := context
    5 [-]: SETTABLE  R3 K1 R2     ; R3["request"] := R2
    6 [-]: GETUPVAL  R3 U0        ; R3 := U0
    7 [-]: GETTABLE  R3 R3 K2     ; R3 := R3["urldecode"]
    8 [-]: SELF      R4 R0 K3     ; R5 := R0; R4 := R0["getenv"]
    9 [-]: LOADK     R6 K4        ; R6 := "PATH_INFO"
   10 [-]: CALL      R4 3 2       ; R4 := R4(R5 to R6)
   11 [-]: TEST      R4 1         ; if not R4 then goto 13 else goto 14
   12 [-]: JMP       1            ; PC += 1 (goto 14)
   13 [-]: LOADK     R4 K5        ; R4 := ""
   14 [-]: LOADBOOL  R5 1 0       ; R5 := true
   15 [-]: CALL      R3 3 2       ; R3 := R3(R4 to R5)
   16 [-]: TEST      R1 0         ; if R1 then goto 18 else goto 27
   17 [-]: JMP       9            ; PC += 9 (goto 27)
   18 [-]: GETGLOBAL R4 K6        ; R4 := ipairs
   19 [-]: MOVE      R5 R1        ; R5 := R1
   20 [-]: CALL      R4 2 4       ; R4 to R6 := R4(R5)
   21 [-]: JMP       3            ; PC += 3 (goto 25)
   22 [-]: LEN       R9 R2        ; R9 := #R2
   23 [-]: ADD       R9 R9 K7     ; R9 := R9 + Unknown_Type_Error
   24 [-]: SETTABLE  R2 R9 R8     ; R2[R9] := R8
   25 [-]: TFORLOOP  R4 2         ; R7 to R8 := R4(R5,R6); if R7 ~= nil then R6 := R7 else goto 27
   26 [-]: JMP       -5           ; PC += -5 (goto 22)
   27 [-]: SELF      R4 R3 K8     ; R5 := R3; R4 := R3["gmatch"]
   28 [-]: LOADK     R6 K9        ; R6 := "[^/]+"
   29 [-]: CALL      R4 3 4       ; R4 to R6 := R4(R5 to R6)
   30 [-]: JMP       3            ; PC += 3 (goto 34)
   31 [-]: LEN       R8 R2        ; R8 := #R2
   32 [-]: ADD       R8 R8 K7     ; R8 := R8 + Unknown_Type_Error
   33 [-]: SETTABLE  R2 R8 R7     ; R2[R8] := R7
   34 [-]: TFORLOOP  R4 1         ; R7 := R4(R5,R6); if R7 ~= nil then R6 := R7 else goto 36
   35 [-]: JMP       -5           ; PC += -5 (goto 31)
   36 [-]: GETUPVAL  R4 U1        ; R4 := U1
   37 [-]: GETTABLE  R4 R4 K10    ; R4 := R4["coxpcall"]
   38 [-]: CLOSURE   R5 0         ; R5 := closure(Function #0_5_0)
   39 [-]: GETGLOBAL R6 K11       ; R6 := error500
   40 [-]: CALL      R4 3 3       ; R4 to R5 := R4(R5 to R6)
   41 [-]: GETUPVAL  R6 U0        ; R6 := U0
   42 [-]: GETTABLE  R6 R6 K12    ; R6 := R6["close"]
   43 [-]: CALL      R6 1 1       ;  := R6()
   44 [-]: RETURN    R0 1         ; return 


; Function:        0_5_0
; Defined at line: 121
; #Upvalues:       0
; #Parameters:     0
; Is_vararg:       0
; Max Stack Size:  2

    0 [-]: GETGLOBAL R0 K0        ; R0 := dispatch
    1 [-]: GETGLOBAL R1 K1        ; R1 := context
    2 [-]: GETTABLE  R1 R1 K2     ; R1 := R1["request"]
    3 [-]: CALL      R0 2 1       ;  := R0(R1)
    4 [-]: RETURN    R0 1         ; return 


; Function:        0_6
; Defined at line: 130
; #Upvalues:       1
; #Parameters:     1
; Is_vararg:       0
; Max Stack Size:  11

    0 [-]: GETGLOBAL R1 K0        ; R1 := type
    1 [-]: MOVE      R2 R0        ; R2 := R0
    2 [-]: CALL      R1 2 2       ; R1 := R1(R2)
    3 [-]: EQ        0 R1 K1      ; if R1 == "table" then goto 5 else goto 45
    4 [-]: JMP       40           ; PC += 40 (goto 45)
    5 [-]: GETGLOBAL R1 K0        ; R1 := type
    6 [-]: GETTABLE  R2 R0 K2     ; R2 := R0["post"]
    7 [-]: CALL      R1 2 2       ; R1 := R1(R2)
    8 [-]: EQ        0 R1 K1      ; if R1 == "table" then goto 10 else goto 39
    9 [-]: JMP       29           ; PC += 29 (goto 39)
   10 [-]: LOADNIL   R1 R3        ; R1 to R3 := nil
   11 [-]: GETGLOBAL R4 K3        ; R4 := pairs
   12 [-]: GETTABLE  R5 R0 K2     ; R5 := R0["post"]
   13 [-]: CALL      R4 2 4       ; R4 to R6 := R4(R5)
   14 [-]: JMP       20           ; PC += 20 (goto 35)
   15 [-]: GETUPVAL  R9 U0        ; R9 := U0
   16 [-]: GETTABLE  R9 R9 K4     ; R9 := R9["formvalue"]
   17 [-]: MOVE      R10 R7       ; R10 := R7
   18 [-]: CALL      R9 2 2       ; R9 := R9(R10)
   19 [-]: MOVE      R3 R9        ; R3 := R9
   20 [-]: GETGLOBAL R9 K0        ; R9 := type
   21 [-]: MOVE      R10 R8       ; R10 := R8
   22 [-]: CALL      R9 2 2       ; R9 := R9(R10)
   23 [-]: EQ        0 R9 K5      ; if R9 == "string" then goto 25 else goto 27
   24 [-]: JMP       2            ; PC += 2 (goto 27)
   25 [-]: EQ        0 R3 R8      ; if R3 == R8 then goto 27 else goto 33
   26 [-]: JMP       6            ; PC += 6 (goto 33)
   27 [-]: EQ        0 R8 K6      ; if R8 == true then goto 29 else goto 35
   28 [-]: JMP       6            ; PC += 6 (goto 35)
   29 [-]: EQ        1 R3 K7      ; if R3 ~= nil then goto 31 else goto 33
   30 [-]: JMP       2            ; PC += 2 (goto 33)
   31 [-]: EQ        0 R3 K8      ; if R3 == "" then goto 33 else goto 35
   32 [-]: JMP       2            ; PC += 2 (goto 35)
   33 [-]: LOADBOOL  R9 0 0       ; R9 := false
   34 [-]: RETURN    R9 2         ; return R9
   35 [-]: TFORLOOP  R4 2         ; R7 to R8 := R4(R5,R6); if R7 ~= nil then R6 := R7 else goto 37
   36 [-]: JMP       -22          ; PC += -22 (goto 15)
   37 [-]: LOADBOOL  R4 1 0       ; R4 := true
   38 [-]: RETURN    R4 2         ; return R4
   39 [-]: GETTABLE  R1 R0 K2     ; R1 := R0["post"]
   40 [-]: EQ        1 R1 K6      ; if R1 ~= true then goto 42 else goto 43
   41 [-]: JMP       1            ; PC += 1 (goto 43)
   42 [-]: LOADBOOL  R1 0 1       ; R1 := false; goto 44
   43 [-]: LOADBOOL  R1 1 0       ; R1 := true
   44 [-]: RETURN    R1 2         ; return R1
   45 [-]: LOADBOOL  R1 0 0       ; R1 := false
   46 [-]: RETURN    R1 2         ; return R1
   47 [-]: RETURN    R0 1         ; return 


; Function:        0_7
; Defined at line: 156
; #Upvalues:       1
; #Parameters:     0
; Is_vararg:       0
; Max Stack Size:  3

    0 [-]: GETUPVAL  R0 U0        ; R0 := U0
    1 [-]: GETTABLE  R0 R0 K0     ; R0 := R0["getenv"]
    2 [-]: LOADK     R1 K1        ; R1 := "REQUEST_METHOD"
    3 [-]: CALL      R0 2 2       ; R0 := R0(R1)
    4 [-]: EQ        1 R0 K2      ; if R0 ~= "POST" then goto 6 else goto 18
    5 [-]: JMP       12           ; PC += 12 (goto 18)
    6 [-]: GETUPVAL  R0 U0        ; R0 := U0
    7 [-]: GETTABLE  R0 R0 K3     ; R0 := R0["status"]
    8 [-]: LOADK     R1 K4        ; R1 := Unknown_Type_Error
    9 [-]: LOADK     R2 K5        ; R2 := "Method Not Allowed"
   10 [-]: CALL      R0 3 1       ;  := R0(R1 to R2)
   11 [-]: GETUPVAL  R0 U0        ; R0 := U0
   12 [-]: GETTABLE  R0 R0 K6     ; R0 := R0["header"]
   13 [-]: LOADK     R1 K7        ; R1 := "Allow"
   14 [-]: LOADK     R2 K2        ; R2 := "POST"
   15 [-]: CALL      R0 3 1       ;  := R0(R1 to R2)
   16 [-]: LOADBOOL  R0 0 0       ; R0 := false
   17 [-]: RETURN    R0 2         ; return R0
   18 [-]: GETGLOBAL R0 K8        ; R0 := context
   19 [-]: GETTABLE  R0 R0 K9     ; R0 := R0["authtoken"]
   20 [-]: EQ        0 R0 K10     ; if R0 == nil then goto 22 else goto 24
   21 [-]: JMP       2            ; PC += 2 (goto 24)
   22 [-]: LOADBOOL  R0 1 0       ; R0 := true
   23 [-]: RETURN    R0 2         ; return R0
   24 [-]: GETUPVAL  R0 U0        ; R0 := U0
   25 [-]: GETTABLE  R0 R0 K11    ; R0 := R0["formvalue"]
   26 [-]: LOADK     R1 K12       ; R1 := "token"
   27 [-]: CALL      R0 2 2       ; R0 := R0(R1)
   28 [-]: GETGLOBAL R1 K8        ; R1 := context
   29 [-]: GETTABLE  R1 R1 K9     ; R1 := R1["authtoken"]
   30 [-]: EQ        1 R0 R1      ; if R0 ~= R1 then goto 32 else goto 44
   31 [-]: JMP       12           ; PC += 12 (goto 44)
   32 [-]: GETUPVAL  R0 U0        ; R0 := U0
   33 [-]: GETTABLE  R0 R0 K3     ; R0 := R0["status"]
   34 [-]: LOADK     R1 K13       ; R1 := Unknown_Type_Error
   35 [-]: LOADK     R2 K14       ; R2 := "Forbidden"
   36 [-]: CALL      R0 3 1       ;  := R0(R1 to R2)
   37 [-]: GETGLOBAL R0 K15       ; R0 := luci
   38 [-]: GETTABLE  R0 R0 K16    ; R0 := R0["template"]
   39 [-]: GETTABLE  R0 R0 K17    ; R0 := R0["render"]
   40 [-]: LOADK     R1 K18       ; R1 := "csrftoken"
   41 [-]: CALL      R0 2 1       ;  := R0(R1)
   42 [-]: LOADBOOL  R0 0 0       ; R0 := false
   43 [-]: RETURN    R0 2         ; return R0
   44 [-]: LOADBOOL  R0 1 0       ; R0 := true
   45 [-]: RETURN    R0 2         ; return R0
   46 [-]: RETURN    R0 1         ; return 


; Function:        0_8
; Defined at line: 176
; #Upvalues:       1
; #Parameters:     2
; Is_vararg:       0
; Max Stack Size:  6

    0 [-]: GETUPVAL  R2 U0        ; R2 := U0
    1 [-]: GETTABLE  R2 R2 K0     ; R2 := R2["ubus"]
    2 [-]: LOADK     R3 K1        ; R3 := "session"
    3 [-]: LOADK     R4 K2        ; R4 := "get"
    4 [-]: NEWTABLE  R5 0 1       ; R5 := {} (size = 0,1)
    5 [-]: SETTABLE  R5 K3 R0     ; R5["ubus_rpc_session"] := R0
    6 [-]: CALL      R2 4 2       ; R2 := R2(R3 to R5)
    7 [-]: GETGLOBAL R3 K4        ; R3 := type
    8 [-]: MOVE      R4 R2        ; R4 := R2
    9 [-]: CALL      R3 2 2       ; R3 := R3(R4)
   10 [-]: EQ        0 R3 K5      ; if R3 == "table" then goto 12 else goto 36
   11 [-]: JMP       24           ; PC += 24 (goto 36)
   12 [-]: GETGLOBAL R3 K4        ; R3 := type
   13 [-]: GETTABLE  R4 R2 K6     ; R4 := R2["values"]
   14 [-]: CALL      R3 2 2       ; R3 := R3(R4)
   15 [-]: EQ        0 R3 K5      ; if R3 == "table" then goto 17 else goto 36
   16 [-]: JMP       19           ; PC += 19 (goto 36)
   17 [-]: GETGLOBAL R3 K4        ; R3 := type
   18 [-]: GETTABLE  R4 R2 K6     ; R4 := R2["values"]
   19 [-]: GETTABLE  R4 R4 K7     ; R4 := R4["token"]
   20 [-]: CALL      R3 2 2       ; R3 := R3(R4)
   21 [-]: EQ        0 R3 K8      ; if R3 == "string" then goto 23 else goto 36
   22 [-]: JMP       13           ; PC += 13 (goto 36)
   23 [-]: TEST      R1 0         ; if R1 then goto 25 else goto 33
   24 [-]: JMP       8            ; PC += 8 (goto 33)
   25 [-]: GETUPVAL  R3 U0        ; R3 := U0
   26 [-]: GETTABLE  R3 R3 K9     ; R3 := R3["contains"]
   27 [-]: MOVE      R4 R1        ; R4 := R1
   28 [-]: GETTABLE  R5 R2 K6     ; R5 := R2["values"]
   29 [-]: GETTABLE  R5 R5 K10    ; R5 := R5["username"]
   30 [-]: CALL      R3 3 2       ; R3 := R3(R4 to R5)
   31 [-]: TEST      R3 0         ; if R3 then goto 33 else goto 36
   32 [-]: JMP       3            ; PC += 3 (goto 36)
   33 [-]: MOVE      R3 R0        ; R3 := R0
   34 [-]: GETTABLE  R4 R2 K6     ; R4 := R2["values"]
   35 [-]: RETURN    R3 3         ; return R3 to R4
   36 [-]: LOADNIL   R3 R4        ; R3 to R4 := nil
   37 [-]: RETURN    R3 3         ; return R3 to R4
   38 [-]: RETURN    R0 1         ; return 


; Function:        0_9
; Defined at line: 191
; #Upvalues:       1
; #Parameters:     1
; Is_vararg:       0
; Max Stack Size:  10

    0 [-]: LOADK     R1 K0        ; R1 := Unknown_Type_Error
    1 [-]: LOADK     R2 K1        ; R2 := "00000000000000000000000000000000"
    2 [-]: GETUPVAL  R3 U0        ; R3 := U0
    3 [-]: GETTABLE  R3 R3 K2     ; R3 := R3["ubus"]
    4 [-]: LOADK     R4 K3        ; R4 := "session"
    5 [-]: LOADK     R5 K4        ; R5 := "get"
    6 [-]: NEWTABLE  R6 0 1       ; R6 := {} (size = 0,1)
    7 [-]: SETTABLE  R6 K5 R2     ; R6["ubus_rpc_session"] := R2
    8 [-]: CALL      R3 4 2       ; R3 := R3(R4 to R6)
    9 [-]: GETGLOBAL R4 K6        ; R4 := type
   10 [-]: MOVE      R5 R3        ; R5 := R3
   11 [-]: CALL      R4 2 2       ; R4 := R4(R5)
   12 [-]: EQ        0 R4 K7      ; if R4 == "table" then goto 14 else goto 28
   13 [-]: JMP       14           ; PC += 14 (goto 28)
   14 [-]: GETGLOBAL R4 K6        ; R4 := type
   15 [-]: GETTABLE  R5 R3 K8     ; R5 := R3["values"]
   16 [-]: CALL      R4 2 2       ; R4 := R4(R5)
   17 [-]: EQ        0 R4 K7      ; if R4 == "table" then goto 19 else goto 28
   18 [-]: JMP       9            ; PC += 9 (goto 28)
   19 [-]: GETGLOBAL R4 K6        ; R4 := type
   20 [-]: GETTABLE  R5 R3 K8     ; R5 := R3["values"]
   21 [-]: GETTABLE  R5 R5 K9     ; R5 := R5["loginerr"]
   22 [-]: CALL      R4 2 2       ; R4 := R4(R5)
   23 [-]: EQ        0 R4 K10     ; if R4 == "number" then goto 25 else goto 28
   24 [-]: JMP       3            ; PC += 3 (goto 28)
   25 [-]: GETTABLE  R4 R3 K8     ; R4 := R3["values"]
   26 [-]: GETTABLE  R4 R4 K9     ; R4 := R4["loginerr"]
   27 [-]: ADD       R1 R4 K0     ; R1 := R4 + Unknown_Type_Error
   28 [-]: GETUPVAL  R4 U0        ; R4 := U0
   29 [-]: GETTABLE  R4 R4 K2     ; R4 := R4["ubus"]
   30 [-]: LOADK     R5 K3        ; R5 := "session"
   31 [-]: LOADK     R6 K11       ; R6 := "set"
   32 [-]: NEWTABLE  R7 0 2       ; R7 := {} (size = 0,2)
   33 [-]: SETTABLE  R7 K5 R2     ; R7["ubus_rpc_session"] := R2
   34 [-]: NEWTABLE  R8 0 2       ; R8 := {} (size = 0,2)
   35 [-]: SETTABLE  R8 K9 R1     ; R8["loginerr"] := R1
   36 [-]: GETGLOBAL R9 K13       ; R9 := os
   37 [-]: GETTABLE  R9 R9 K14    ; R9 := R9["time"]
   38 [-]: CALL      R9 1 2       ; R9 := R9()
   39 [-]: SETTABLE  R8 K12 R9    ; R8["timestamp"] := R9
   40 [-]: SETTABLE  R7 K8 R8     ; R7["values"] := R8
   41 [-]: CALL      R4 4 1       ;  := R4(R5 to R7)
   42 [-]: RETURN    R0 1         ; return 


; Function:        0_10
; Defined at line: 209
; #Upvalues:       1
; #Parameters:     1
; Is_vararg:       0
; Max Stack Size:  9

    0 [-]: LOADK     R1 K0        ; R1 := "00000000000000000000000000000000"
    1 [-]: GETUPVAL  R2 U0        ; R2 := U0
    2 [-]: GETTABLE  R2 R2 K1     ; R2 := R2["ubus"]
    3 [-]: LOADK     R3 K2        ; R3 := "session"
    4 [-]: LOADK     R4 K3        ; R4 := "unset"
    5 [-]: NEWTABLE  R5 0 2       ; R5 := {} (size = 0,2)
    6 [-]: SETTABLE  R5 K4 R1     ; R5["ubus_rpc_session"] := R1
    7 [-]: NEWTABLE  R6 2 0       ; R6 := {} (size = 2,0)
    8 [-]: LOADK     R7 K6        ; R7 := "loginerr"
    9 [-]: LOADK     R8 K7        ; R8 := "timestamp"
   10 [-]: SETLIST   R6 2 1       ; R6[0] to R6[1] := R7 to R8 ; R(a)[(c-1)*FPF+i] := R(a+i), 1 <= i <= b, a=6, b=2, c=1, FPF=50
   11 [-]: SETTABLE  R5 K5 R6     ; R5["keys"] := R6
   12 [-]: CALL      R2 4 1       ;  := R2(R3 to R5)
   13 [-]: RETURN    R0 1         ; return 


; Function:        0_11
; Defined at line: 218
; #Upvalues:       1
; #Parameters:     1
; Is_vararg:       0
; Max Stack Size:  6

    0 [-]: LOADK     R1 K0        ; R1 := "00000000000000000000000000000000"
    1 [-]: GETUPVAL  R2 U0        ; R2 := U0
    2 [-]: GETTABLE  R2 R2 K1     ; R2 := R2["ubus"]
    3 [-]: LOADK     R3 K2        ; R3 := "session"
    4 [-]: LOADK     R4 K3        ; R4 := "get"
    5 [-]: NEWTABLE  R5 0 1       ; R5 := {} (size = 0,1)
    6 [-]: SETTABLE  R5 K4 R1     ; R5["ubus_rpc_session"] := R1
    7 [-]: CALL      R2 4 2       ; R2 := R2(R3 to R5)
    8 [-]: GETGLOBAL R3 K5        ; R3 := type
    9 [-]: MOVE      R4 R2        ; R4 := R2
   10 [-]: CALL      R3 2 2       ; R3 := R3(R4)
   11 [-]: EQ        0 R3 K6      ; if R3 == "table" then goto 13 else goto 47
   12 [-]: JMP       34           ; PC += 34 (goto 47)
   13 [-]: GETGLOBAL R3 K5        ; R3 := type
   14 [-]: GETTABLE  R4 R2 K7     ; R4 := R2["values"]
   15 [-]: CALL      R3 2 2       ; R3 := R3(R4)
   16 [-]: EQ        0 R3 K6      ; if R3 == "table" then goto 18 else goto 47
   17 [-]: JMP       29           ; PC += 29 (goto 47)
   18 [-]: GETGLOBAL R3 K5        ; R3 := type
   19 [-]: GETTABLE  R4 R2 K7     ; R4 := R2["values"]
   20 [-]: GETTABLE  R4 R4 K8     ; R4 := R4["loginerr"]
   21 [-]: CALL      R3 2 2       ; R3 := R3(R4)
   22 [-]: EQ        0 R3 K9      ; if R3 == "number" then goto 24 else goto 47
   23 [-]: JMP       23           ; PC += 23 (goto 47)
   24 [-]: GETTABLE  R3 R2 K7     ; R3 := R2["values"]
   25 [-]: GETTABLE  R3 R3 K8     ; R3 := R3["loginerr"]
   26 [-]: GETGLOBAL R4 K10       ; R4 := tonumber
   27 [-]: GETGLOBAL R5 K11       ; R5 := luci
   28 [-]: GETTABLE  R5 R5 K12    ; R5 := R5["config"]
   29 [-]: GETTABLE  R5 R5 K13    ; R5 := R5["sauth"]
   30 [-]: GETTABLE  R5 R5 K14    ; R5 := R5["errlimit"]
   31 [-]: TEST      R5 1         ; if not R5 then goto 33 else goto 34
   32 [-]: JMP       1            ; PC += 1 (goto 34)
   33 [-]: LOADK     R5 K15       ; R5 := Unknown_Type_Error
   34 [-]: CALL      R4 2 2       ; R4 := R4(R5)
   35 [-]: LE        0 R4 R3      ; if R4 <= R3 then goto 37 else goto 47
   36 [-]: JMP       10           ; PC += 10 (goto 47)
   37 [-]: GETTABLE  R3 R2 K7     ; R3 := R2["values"]
   38 [-]: GETTABLE  R3 R3 K16    ; R3 := R3["timestamp"]
   39 [-]: ADD       R3 R3 K17    ; R3 := R3 + Unknown_Type_Error
   40 [-]: GETGLOBAL R4 K18       ; R4 := os
   41 [-]: GETTABLE  R4 R4 K19    ; R4 := R4["time"]
   42 [-]: CALL      R4 1 2       ; R4 := R4()
   43 [-]: LT        0 R4 R3      ; if R4 < R3 then goto 45 else goto 47
   44 [-]: JMP       2            ; PC += 2 (goto 47)
   45 [-]: LOADBOOL  R3 1 0       ; R3 := true
   46 [-]: RETURN    R3 2         ; return R3
   47 [-]: LOADBOOL  R3 0 0       ; R3 := false
   48 [-]: RETURN    R3 2         ; return R3
   49 [-]: RETURN    R0 1         ; return 


; Function:        0_12
; Defined at line: 234
; #Upvalues:       1
; #Parameters:     1
; Is_vararg:       0
; Max Stack Size:  11

    0 [-]: GETGLOBAL R1 K0        ; R1 := require
    1 [-]: LOADK     R2 K1        ; R2 := "luci.util"
    2 [-]: CALL      R1 2 2       ; R1 := R1(R2)
    3 [-]: LOADK     R2 K2        ; R2 := "00000000000000000000000000000000"
    4 [-]: GETTABLE  R3 R1 K3     ; R3 := R1["ubus"]
    5 [-]: LOADK     R4 K4        ; R4 := "session"
    6 [-]: LOADK     R5 K5        ; R5 := "get"
    7 [-]: NEWTABLE  R6 0 1       ; R6 := {} (size = 0,1)
    8 [-]: SETTABLE  R6 K6 R2     ; R6["ubus_rpc_session"] := R2
    9 [-]: CALL      R3 4 2       ; R3 := R3(R4 to R6)
   10 [-]: LOADNIL   R4 R4        ; R4 := nil
   11 [-]: GETGLOBAL R5 K7        ; R5 := type
   12 [-]: MOVE      R6 R3        ; R6 := R3
   13 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   14 [-]: EQ        0 R5 K8      ; if R5 == "table" then goto 16 else goto 42
   15 [-]: JMP       26           ; PC += 26 (goto 42)
   16 [-]: GETGLOBAL R5 K7        ; R5 := type
   17 [-]: GETTABLE  R6 R3 K9     ; R6 := R3["values"]
   18 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   19 [-]: EQ        0 R5 K8      ; if R5 == "table" then goto 21 else goto 42
   20 [-]: JMP       21           ; PC += 21 (goto 42)
   21 [-]: GETGLOBAL R5 K7        ; R5 := type
   22 [-]: GETTABLE  R6 R3 K9     ; R6 := R3["values"]
   23 [-]: GETTABLE  R6 R6 K10    ; R6 := R6["token"]
   24 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   25 [-]: EQ        0 R5 K11     ; if R5 == "string" then goto 27 else goto 42
   26 [-]: JMP       15           ; PC += 15 (goto 42)
   27 [-]: GETGLOBAL R5 K7        ; R5 := type
   28 [-]: GETTABLE  R6 R3 K9     ; R6 := R3["values"]
   29 [-]: GETTABLE  R6 R6 K12    ; R6 := R6["expired"]
   30 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   31 [-]: EQ        0 R5 K13     ; if R5 == "number" then goto 33 else goto 42
   32 [-]: JMP       9            ; PC += 9 (goto 42)
   33 [-]: GETTABLE  R5 R3 K9     ; R5 := R3["values"]
   34 [-]: GETTABLE  R5 R5 K12    ; R5 := R5["expired"]
   35 [-]: GETGLOBAL R6 K14       ; R6 := os
   36 [-]: GETTABLE  R6 R6 K15    ; R6 := R6["time"]
   37 [-]: CALL      R6 1 2       ; R6 := R6()
   38 [-]: LT        0 R6 R5      ; if R6 < R5 then goto 40 else goto 42
   39 [-]: JMP       2            ; PC += 2 (goto 42)
   40 [-]: GETTABLE  R5 R3 K9     ; R5 := R3["values"]
   41 [-]: GETTABLE  R4 R5 K10    ; R4 := R5["token"]
   42 [-]: TEST      R4 1         ; if not R4 then goto 44 else goto 63
   43 [-]: JMP       19           ; PC += 19 (goto 63)
   44 [-]: GETUPVAL  R5 U0        ; R5 := U0
   45 [-]: GETTABLE  R5 R5 K16    ; R5 := R5["uniqueid"]
   46 [-]: LOADK     R6 K17       ; R6 := Unknown_Type_Error
   47 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   48 [-]: MOVE      R4 R5        ; R4 := R5
   49 [-]: GETTABLE  R5 R1 K3     ; R5 := R1["ubus"]
   50 [-]: LOADK     R6 K4        ; R6 := "session"
   51 [-]: LOADK     R7 K18       ; R7 := "set"
   52 [-]: NEWTABLE  R8 0 2       ; R8 := {} (size = 0,2)
   53 [-]: SETTABLE  R8 K6 R2     ; R8["ubus_rpc_session"] := R2
   54 [-]: NEWTABLE  R9 0 2       ; R9 := {} (size = 0,2)
   55 [-]: SETTABLE  R9 K10 R4    ; R9["token"] := R4
   56 [-]: GETGLOBAL R10 K14      ; R10 := os
   57 [-]: GETTABLE  R10 R10 K15  ; R10 := R10["time"]
   58 [-]: CALL      R10 1 2      ; R10 := R10()
   59 [-]: ADD       R10 R10 K19  ; R10 := R10 + Unknown_Type_Error
   60 [-]: SETTABLE  R9 K12 R10   ; R9["expired"] := R10
   61 [-]: SETTABLE  R8 K9 R9     ; R8["values"] := R9
   62 [-]: CALL      R5 4 1       ;  := R5(R6 to R8)
   63 [-]: RETURN    R4 2         ; return R4
   64 [-]: RETURN    R0 1         ; return 


; Function:        0_13
; Defined at line: 261
; #Upvalues:       1
; #Parameters:     0
; Is_vararg:       0
; Max Stack Size:  4

    0 [-]: GETGLOBAL R0 K0        ; R0 := luci
    1 [-]: GETTABLE  R0 R0 K1     ; R0 := R0["config"]
    2 [-]: GETTABLE  R0 R0 K2     ; R0 := R0["main"]
    3 [-]: GETTABLE  R0 R0 K3     ; R0 := R0["factory"]
    4 [-]: EQ        0 R0 K4      ; if R0 == "1" then goto 6 else goto 8
    5 [-]: JMP       2            ; PC += 2 (goto 8)
    6 [-]: LOADBOOL  R1 1 0       ; R1 := true
    7 [-]: RETURN    R1 2         ; return R1
    8 [-]: GETUPVAL  R1 U0        ; R1 := U0
    9 [-]: GETTABLE  R1 R1 K5     ; R1 := R1["exec"]
   10 [-]: LOADK     R2 K6        ; R2 := "bdinfo factory"
   11 [-]: CALL      R1 2 2       ; R1 := R1(R2)
   12 [-]: MOVE      R0 R1        ; R0 := R1
   13 [-]: EQ        0 R0 K4      ; if R0 == "1" then goto 15 else goto 17
   14 [-]: JMP       2            ; PC += 2 (goto 17)
   15 [-]: LOADBOOL  R1 0 0       ; R1 := false
   16 [-]: RETURN    R1 2         ; return R1
   17 [-]: GETUPVAL  R1 U0        ; R1 := U0
   18 [-]: GETTABLE  R1 R1 K5     ; R1 := R1["exec"]
   19 [-]: LOADK     R2 K7        ; R2 := "bdinfo check | tr -d \'\n\'"
   20 [-]: CALL      R1 2 2       ; R1 := R1(R2)
   21 [-]: GETUPVAL  R2 U0        ; R2 := U0
   22 [-]: GETTABLE  R2 R2 K5     ; R2 := R2["exec"]
   23 [-]: LOADK     R3 K8        ; R3 := "bdinfo checkuuid | tr -d \'\n\'"
   24 [-]: CALL      R2 2 2       ; R2 := R2(R3)
   25 [-]: EQ        0 R1 K9      ; if R1 == "OK" then goto 27 else goto 29
   26 [-]: JMP       2            ; PC += 2 (goto 29)
   27 [-]: EQ        1 R2 K9      ; if R2 ~= "OK" then goto 29 else goto 30
   28 [-]: JMP       1            ; PC += 1 (goto 30)
   29 [-]: LOADBOOL  R3 0 1       ; R3 := false; goto 31
   30 [-]: LOADBOOL  R3 1 0       ; R3 := true
   31 [-]: RETURN    R3 2         ; return R3
   32 [-]: RETURN    R0 1         ; return 


; Function:        0_14
; Defined at line: 277
; #Upvalues:       3
; #Parameters:     1
; Is_vararg:       0
; Max Stack Size:  7

    0 [-]: LOADK     R1 K0        ; R1 := "00000000000000000000000000000000"
    1 [-]: GETUPVAL  R2 U0        ; R2 := U0
    2 [-]: GETTABLE  R2 R2 K1     ; R2 := R2["ubus"]
    3 [-]: LOADK     R3 K2        ; R3 := "session"
    4 [-]: LOADK     R4 K3        ; R4 := "get"
    5 [-]: NEWTABLE  R5 0 1       ; R5 := {} (size = 0,1)
    6 [-]: SETTABLE  R5 K4 R1     ; R5["ubus_rpc_session"] := R1
    7 [-]: CALL      R2 4 2       ; R2 := R2(R3 to R5)
    8 [-]: GETUPVAL  R3 U1        ; R3 := U1
    9 [-]: GETTABLE  R3 R3 K5     ; R3 := R3["formvalue"]
   10 [-]: LOADK     R4 K6        ; R4 := "salt"
   11 [-]: CALL      R3 2 2       ; R3 := R3(R4)
   12 [-]: GETUPVAL  R4 U1        ; R4 := U1
   13 [-]: GETTABLE  R4 R4 K5     ; R4 := R4["formvalue"]
   14 [-]: LOADK     R5 K7        ; R5 := "token"
   15 [-]: CALL      R4 2 2       ; R4 := R4(R5)
   16 [-]: GETUPVAL  R5 U2        ; R5 := U2
   17 [-]: GETTABLE  R5 R5 K8     ; R5 := R5["user"]
   18 [-]: GETTABLE  R5 R5 K9     ; R5 := R5["getpasswd"]
   19 [-]: MOVE      R6 R0        ; R6 := R0
   20 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   21 [-]: TEST      R5 0         ; if R5 then goto 23 else goto 25
   22 [-]: JMP       2            ; PC += 2 (goto 25)
   23 [-]: LOADBOOL  R5 1 0       ; R5 := true
   24 [-]: RETURN    R5 2         ; return R5
   25 [-]: GETGLOBAL R5 K10       ; R5 := luci
   26 [-]: GETTABLE  R5 R5 K11    ; R5 := R5["config"]
   27 [-]: GETTABLE  R5 R5 K12    ; R5 := R5["sauth"]
   28 [-]: GETTABLE  R5 R5 K6     ; R5 := R5["salt"]
   29 [-]: EQ        1 R3 R5      ; if R3 ~= R5 then goto 31 else goto 33
   30 [-]: JMP       2            ; PC += 2 (goto 33)
   31 [-]: LOADBOOL  R5 0 0       ; R5 := false
   32 [-]: RETURN    R5 2         ; return R5
   33 [-]: TEST      R4 1         ; if not R4 then goto 35 else goto 49
   34 [-]: JMP       14           ; PC += 14 (goto 49)
   35 [-]: GETGLOBAL R5 K10       ; R5 := luci
   36 [-]: GETTABLE  R5 R5 K11    ; R5 := R5["config"]
   37 [-]: GETTABLE  R5 R5 K12    ; R5 := R5["sauth"]
   38 [-]: GETTABLE  R5 R5 K13    ; R5 := R5["defpasswd"]
   39 [-]: EQ        1 R5 K14     ; if R5 ~= "1" then goto 41 else goto 47
   40 [-]: JMP       6            ; PC += 6 (goto 47)
   41 [-]: GETGLOBAL R5 K10       ; R5 := luci
   42 [-]: GETTABLE  R5 R5 K11    ; R5 := R5["config"]
   43 [-]: GETTABLE  R5 R5 K12    ; R5 := R5["sauth"]
   44 [-]: GETTABLE  R5 R5 R0     ; R5 := R5[R0]
   45 [-]: TEST      R5 1         ; if not R5 then goto 47 else goto 49
   46 [-]: JMP       2            ; PC += 2 (goto 49)
   47 [-]: LOADBOOL  R5 1 0       ; R5 := true
   48 [-]: RETURN    R5 2         ; return R5
   49 [-]: GETGLOBAL R5 K15       ; R5 := type
   50 [-]: MOVE      R6 R2        ; R6 := R2
   51 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   52 [-]: EQ        0 R5 K16     ; if R5 == "table" then goto 54 else goto 71
   53 [-]: JMP       17           ; PC += 17 (goto 71)
   54 [-]: GETGLOBAL R5 K15       ; R5 := type
   55 [-]: GETTABLE  R6 R2 K17    ; R6 := R2["values"]
   56 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   57 [-]: EQ        0 R5 K16     ; if R5 == "table" then goto 59 else goto 71
   58 [-]: JMP       12           ; PC += 12 (goto 71)
   59 [-]: GETGLOBAL R5 K15       ; R5 := type
   60 [-]: GETTABLE  R6 R2 K17    ; R6 := R2["values"]
   61 [-]: GETTABLE  R6 R6 K7     ; R6 := R6["token"]
   62 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   63 [-]: EQ        0 R5 K18     ; if R5 == "string" then goto 65 else goto 71
   64 [-]: JMP       6            ; PC += 6 (goto 71)
   65 [-]: GETTABLE  R5 R2 K17    ; R5 := R2["values"]
   66 [-]: GETTABLE  R5 R5 K7     ; R5 := R5["token"]
   67 [-]: EQ        0 R4 R5      ; if R4 == R5 then goto 69 else goto 71
   68 [-]: JMP       2            ; PC += 2 (goto 71)
   69 [-]: LOADBOOL  R5 1 0       ; R5 := true
   70 [-]: RETURN    R5 2         ; return R5
   71 [-]: LOADBOOL  R5 0 0       ; R5 := false
   72 [-]: RETURN    R5 2         ; return R5
   73 [-]: RETURN    R0 1         ; return 


; Function:        0_15
; Defined at line: 307
; #Upvalues:       3
; #Parameters:     2
; Is_vararg:       0
; Max Stack Size:  13

    0 [-]: LOADK     R2 K0        ; R2 := "00000000000000000000000000000000"
    1 [-]: GETUPVAL  R3 U0        ; R3 := U0
    2 [-]: GETTABLE  R3 R3 K1     ; R3 := R3["formvalue"]
    3 [-]: LOADK     R4 K2        ; R4 := "token"
    4 [-]: CALL      R3 2 2       ; R3 := R3(R4)
    5 [-]: LOADBOOL  R4 0 0       ; R4 := false
    6 [-]: GETUPVAL  R5 U1        ; R5 := U1
    7 [-]: GETTABLE  R5 R5 K3     ; R5 := R5["user"]
    8 [-]: GETTABLE  R5 R5 K4     ; R5 := R5["getpasswd"]
    9 [-]: MOVE      R6 R0        ; R6 := R0
   10 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   11 [-]: TEST      R5 0         ; if R5 then goto 13 else goto 15
   12 [-]: JMP       2            ; PC += 2 (goto 15)
   13 [-]: LOADBOOL  R5 1 0       ; R5 := true
   14 [-]: RETURN    R5 2         ; return R5
   15 [-]: TEST      R1 0         ; if R1 then goto 17 else goto 25
   16 [-]: JMP       8            ; PC += 8 (goto 25)
   17 [-]: LEN       R5 R1        ; R5 := #R1
   18 [-]: EQ        0 R5 K5      ; if R5 == Unknown_Type_Error then goto 20 else goto 25
   19 [-]: JMP       5            ; PC += 5 (goto 25)
   20 [-]: SELF      R5 R1 K6     ; R6 := R1; R5 := R1["match"]
   21 [-]: LOADK     R7 K7        ; R7 := "^[a-fA-F0-9]+$"
   22 [-]: CALL      R5 3 2       ; R5 := R5(R6 to R7)
   23 [-]: EQ        0 R5 K8      ; if R5 == nil then goto 25 else goto 27
   24 [-]: JMP       2            ; PC += 2 (goto 27)
   25 [-]: LOADBOOL  R5 0 0       ; R5 := false
   26 [-]: RETURN    R5 2         ; return R5
   27 [-]: GETGLOBAL R5 K9        ; R5 := luci
   28 [-]: GETTABLE  R5 R5 K10    ; R5 := R5["config"]
   29 [-]: GETTABLE  R5 R5 K11    ; R5 := R5["sauth"]
   30 [-]: GETTABLE  R5 R5 K12    ; R5 := R5["defpasswd"]
   31 [-]: EQ        1 R5 K13     ; if R5 ~= "1" then goto 33 else goto 39
   32 [-]: JMP       6            ; PC += 6 (goto 39)
   33 [-]: GETGLOBAL R5 K9        ; R5 := luci
   34 [-]: GETTABLE  R5 R5 K10    ; R5 := R5["config"]
   35 [-]: GETTABLE  R5 R5 K11    ; R5 := R5["sauth"]
   36 [-]: GETTABLE  R5 R5 R0     ; R5 := R5[R0]
   37 [-]: TEST      R5 1         ; if not R5 then goto 39 else goto 68
   38 [-]: JMP       29           ; PC += 29 (goto 68)
   39 [-]: GETGLOBAL R5 K14       ; R5 := require
   40 [-]: LOADK     R6 K15       ; R6 := "luci.model.uci"
   41 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   42 [-]: GETTABLE  R5 R5 K16    ; R5 := R5["cursor"]
   43 [-]: CALL      R5 1 2       ; R5 := R5()
   44 [-]: GETUPVAL  R6 U2        ; R6 := U2
   45 [-]: GETTABLE  R6 R6 K17    ; R6 := R6["exec"]
   46 [-]: LOADK     R7 K18       ; R7 := "echo -n \'"
   47 [-]: MOVE      R8 R1        ; R8 := R1
   48 [-]: LOADK     R9 K19       ; R9 := "\' | crypt -ea"
   49 [-]: CONCAT    R7 R7 R9     ; R7 := concat(R7 to R9)
   50 [-]: CALL      R6 2 2       ; R6 := R6(R7)
   51 [-]: SELF      R7 R5 K20    ; R8 := R5; R7 := R5["set"]
   52 [-]: LOADK     R9 K9        ; R9 := "luci"
   53 [-]: LOADK     R10 K11      ; R10 := "sauth"
   54 [-]: MOVE      R11 R0       ; R11 := R0
   55 [-]: MOVE      R12 R6       ; R12 := R6
   56 [-]: CALL      R7 6 1       ;  := R7(R8 to R12)
   57 [-]: SELF      R7 R5 K20    ; R8 := R5; R7 := R5["set"]
   58 [-]: LOADK     R9 K9        ; R9 := "luci"
   59 [-]: LOADK     R10 K11      ; R10 := "sauth"
   60 [-]: LOADK     R11 K12      ; R11 := "defpasswd"
   61 [-]: LOADK     R12 K21      ; R12 := "0"
   62 [-]: CALL      R7 6 1       ;  := R7(R8 to R12)
   63 [-]: SELF      R7 R5 K22    ; R8 := R5; R7 := R5["commit"]
   64 [-]: LOADK     R9 K9        ; R9 := "luci"
   65 [-]: CALL      R7 3 1       ;  := R7(R8 to R9)
   66 [-]: LOADBOOL  R4 1 0       ; R4 := true
   67 [-]: JMP       23           ; PC += 23 (goto 91)
   68 [-]: TEST      R3 0         ; if R3 then goto 70 else goto 91
   69 [-]: JMP       21           ; PC += 21 (goto 91)
   70 [-]: GETUPVAL  R5 U2        ; R5 := U2
   71 [-]: GETTABLE  R5 R5 K17    ; R5 := R5["exec"]
   72 [-]: LOADK     R6 K18       ; R6 := "echo -n \'"
   73 [-]: GETGLOBAL R7 K9        ; R7 := luci
   74 [-]: GETTABLE  R7 R7 K10    ; R7 := R7["config"]
   75 [-]: GETTABLE  R7 R7 K11    ; R7 := R7["sauth"]
   76 [-]: GETTABLE  R7 R7 R0     ; R7 := R7[R0]
   77 [-]: LOADK     R8 K23       ; R8 := "\' | crypt -da"
   78 [-]: CONCAT    R6 R6 R8     ; R6 := concat(R6 to R8)
   79 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   80 [-]: GETUPVAL  R6 U2        ; R6 := U2
   81 [-]: GETTABLE  R6 R6 K17    ; R6 := R6["exec"]
   82 [-]: LOADK     R7 K18       ; R7 := "echo -n \'"
   83 [-]: MOVE      R8 R5        ; R8 := R5
   84 [-]: MOVE      R9 R3        ; R9 := R3
   85 [-]: LOADK     R10 K24      ; R10 := "\' | sha256sum | cut -c 1-64 | tr -d \'\n\'"
   86 [-]: CONCAT    R7 R7 R10    ; R7 := concat(R7 to R10)
   87 [-]: CALL      R6 2 2       ; R6 := R6(R7)
   88 [-]: EQ        0 R1 R6      ; if R1 == R6 then goto 90 else goto 91
   89 [-]: JMP       1            ; PC += 1 (goto 91)
   90 [-]: LOADBOOL  R4 1 0       ; R4 := true
   91 [-]: GETUPVAL  R5 U2        ; R5 := U2
   92 [-]: GETTABLE  R5 R5 K25    ; R5 := R5["ubus"]
   93 [-]: LOADK     R6 K26       ; R6 := "session"
   94 [-]: LOADK     R7 K27       ; R7 := "unset"
   95 [-]: NEWTABLE  R8 0 2       ; R8 := {} (size = 0,2)
   96 [-]: SETTABLE  R8 K28 R2    ; R8["ubus_rpc_session"] := R2
   97 [-]: NEWTABLE  R9 2 0       ; R9 := {} (size = 2,0)
   98 [-]: LOADK     R10 K2       ; R10 := "token"
   99 [-]: LOADK     R11 K30      ; R11 := "expired"
  100 [-]: SETLIST   R9 2 1       ; R9[0] to R9[1] := R10 to R11 ; R(a)[(c-1)*FPF+i] := R(a+i), 1 <= i <= b, a=9, b=2, c=1, FPF=50
  101 [-]: SETTABLE  R8 K29 R9    ; R8["keys"] := R9
  102 [-]: CALL      R5 4 1       ;  := R5(R6 to R8)
  103 [-]: RETURN    R4 2         ; return R4
  104 [-]: RETURN    R0 1         ; return 


; Function:        0_16
; Defined at line: 344
; #Upvalues:       7
; #Parameters:     3
; Is_vararg:       0
; Max Stack Size:  11

    0 [-]: GETUPVAL  R3 U0        ; R3 := U0
    1 [-]: GETTABLE  R3 R3 K0     ; R3 := R3["contains"]
    2 [-]: MOVE      R4 R2        ; R4 := R2
    3 [-]: MOVE      R5 R0        ; R5 := R0
    4 [-]: CALL      R3 3 2       ; R3 := R3(R4 to R5)
    5 [-]: TEST      R3 0         ; if R3 then goto 7 else goto 68
    6 [-]: JMP       61           ; PC += 61 (goto 68)
    7 [-]: GETGLOBAL R3 K1        ; R3 := test_post_security
    8 [-]: CALL      R3 1 2       ; R3 := R3()
    9 [-]: TEST      R3 0         ; if R3 then goto 11 else goto 68
   10 [-]: JMP       57           ; PC += 57 (goto 68)
   11 [-]: GETUPVAL  R3 U1        ; R3 := U1
   12 [-]: CALL      R3 1 2       ; R3 := R3()
   13 [-]: TEST      R3 0         ; if R3 then goto 15 else goto 68
   14 [-]: JMP       53           ; PC += 53 (goto 68)
   15 [-]: GETUPVAL  R3 U2        ; R3 := U2
   16 [-]: MOVE      R4 R0        ; R4 := R0
   17 [-]: MOVE      R5 R1        ; R5 := R1
   18 [-]: CALL      R3 3 2       ; R3 := R3(R4 to R5)
   19 [-]: TEST      R3 0         ; if R3 then goto 21 else goto 68
   20 [-]: JMP       47           ; PC += 47 (goto 68)
   21 [-]: GETUPVAL  R3 U0        ; R3 := U0
   22 [-]: GETTABLE  R3 R3 K2     ; R3 := R3["ubus"]
   23 [-]: LOADK     R4 K3        ; R4 := "session"
   24 [-]: LOADK     R5 K4        ; R5 := "login"
   25 [-]: NEWTABLE  R6 0 3       ; R6 := {} (size = 0,3)
   26 [-]: SETTABLE  R6 K5 R0     ; R6["username"] := R0
   27 [-]: SETTABLE  R6 K6 R1     ; R6["password"] := R1
   28 [-]: GETGLOBAL R7 K8        ; R7 := tonumber
   29 [-]: GETGLOBAL R8 K9        ; R8 := luci
   30 [-]: GETTABLE  R8 R8 K10    ; R8 := R8["config"]
   31 [-]: GETTABLE  R8 R8 K11    ; R8 := R8["sauth"]
   32 [-]: GETTABLE  R8 R8 K12    ; R8 := R8["sessiontime"]
   33 [-]: CALL      R7 2 2       ; R7 := R7(R8)
   34 [-]: SETTABLE  R6 K7 R7     ; R6["timeout"] := R7
   35 [-]: CALL      R3 4 2       ; R3 := R3(R4 to R6)
   36 [-]: GETGLOBAL R4 K13       ; R4 := type
   37 [-]: MOVE      R5 R3        ; R5 := R3
   38 [-]: CALL      R4 2 2       ; R4 := R4(R5)
   39 [-]: EQ        0 R4 K14     ; if R4 == "table" then goto 41 else goto 68
   40 [-]: JMP       27           ; PC += 27 (goto 68)
   41 [-]: GETGLOBAL R4 K13       ; R4 := type
   42 [-]: GETTABLE  R5 R3 K15    ; R5 := R3["ubus_rpc_session"]
   43 [-]: CALL      R4 2 2       ; R4 := R4(R5)
   44 [-]: EQ        0 R4 K16     ; if R4 == "string" then goto 46 else goto 68
   45 [-]: JMP       22           ; PC += 22 (goto 68)
   46 [-]: GETUPVAL  R4 U0        ; R4 := U0
   47 [-]: GETTABLE  R4 R4 K2     ; R4 := R4["ubus"]
   48 [-]: LOADK     R5 K3        ; R5 := "session"
   49 [-]: LOADK     R6 K17       ; R6 := "set"
   50 [-]: NEWTABLE  R7 0 2       ; R7 := {} (size = 0,2)
   51 [-]: GETTABLE  R8 R3 K15    ; R8 := R3["ubus_rpc_session"]
   52 [-]: SETTABLE  R7 K15 R8    ; R7["ubus_rpc_session"] := R8
   53 [-]: NEWTABLE  R8 0 1       ; R8 := {} (size = 0,1)
   54 [-]: GETUPVAL  R9 U3        ; R9 := U3
   55 [-]: GETTABLE  R9 R9 K20    ; R9 := R9["uniqueid"]
   56 [-]: LOADK     R10 K21      ; R10 := Unknown_Type_Error
   57 [-]: CALL      R9 2 2       ; R9 := R9(R10)
   58 [-]: SETTABLE  R8 K19 R9    ; R8["token"] := R9
   59 [-]: SETTABLE  R7 K18 R8    ; R7["values"] := R8
   60 [-]: CALL      R4 4 1       ;  := R4(R5 to R7)
   61 [-]: GETUPVAL  R4 U4        ; R4 := U4
   62 [-]: MOVE      R5 R0        ; R5 := R0
   63 [-]: CALL      R4 2 1       ;  := R4(R5)
   64 [-]: GETUPVAL  R4 U5        ; R4 := U5
   65 [-]: GETTABLE  R5 R3 K15    ; R5 := R3["ubus_rpc_session"]
   66 [-]: TAILCALL  R4 2 0       ; R4 to top := R4(R5)
   67 [-]: RETURN    R4 0         ; return R4 to top
   68 [-]: GETUPVAL  R3 U6        ; R3 := U6
   69 [-]: MOVE      R4 R0        ; R4 := R0
   70 [-]: CALL      R3 2 1       ;  := R3(R4)
   71 [-]: LOADNIL   R3 R4        ; R3 to R4 := nil
   72 [-]: RETURN    R3 3         ; return R3 to R4
   73 [-]: RETURN    R0 1         ; return 


; Function:        0_17
; Defined at line: 369
; #Upvalues:       0
; #Parameters:     0
; Is_vararg:       0
; Max Stack Size:  14

    0 [-]: GETGLOBAL R0 K0        ; R0 := require
    1 [-]: LOADK     R1 K1        ; R1 := "luci.model.uci"
    2 [-]: CALL      R0 2 2       ; R0 := R0(R1)
    3 [-]: GETTABLE  R0 R0 K2     ; R0 := R0["cursor"]
    4 [-]: CALL      R0 1 2       ; R0 := R0()
    5 [-]: SELF      R1 R0 K3     ; R2 := R0; R1 := R0["get"]
    6 [-]: LOADK     R3 K4        ; R3 := "system"
    7 [-]: LOADK     R4 K5        ; R4 := "@system[0]"
    8 [-]: LOADK     R5 K6        ; R5 := "domain"
    9 [-]: CALL      R1 5 2       ; R1 := R1(R2 to R5)
   10 [-]: TEST      R1 0         ; if R1 then goto 12 else goto 84
   11 [-]: JMP       72           ; PC += 72 (goto 84)
   12 [-]: GETGLOBAL R2 K0        ; R2 := require
   13 [-]: LOADK     R3 K7        ; R3 := "luci.cbi.datatypes"
   14 [-]: CALL      R2 2 2       ; R2 := R2(R3)
   15 [-]: GETGLOBAL R3 K8        ; R3 := luci
   16 [-]: GETTABLE  R3 R3 K9     ; R3 := R3["http"]
   17 [-]: GETTABLE  R3 R3 K10    ; R3 := R3["getenv"]
   18 [-]: LOADK     R4 K11       ; R4 := "SERVER_ADDR"
   19 [-]: CALL      R3 2 2       ; R3 := R3(R4)
   20 [-]: GETGLOBAL R4 K8        ; R4 := luci
   21 [-]: GETTABLE  R4 R4 K9     ; R4 := R4["http"]
   22 [-]: GETTABLE  R4 R4 K10    ; R4 := R4["getenv"]
   23 [-]: LOADK     R5 K12       ; R5 := "HTTP_HOST"
   24 [-]: CALL      R4 2 2       ; R4 := R4(R5)
   25 [-]: GETGLOBAL R5 K8        ; R5 := luci
   26 [-]: GETTABLE  R5 R5 K9     ; R5 := R5["http"]
   27 [-]: GETTABLE  R5 R5 K10    ; R5 := R5["getenv"]
   28 [-]: LOADK     R6 K13       ; R6 := "REQUEST_URI"
   29 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   30 [-]: GETGLOBAL R6 K8        ; R6 := luci
   31 [-]: GETTABLE  R6 R6 K9     ; R6 := R6["http"]
   32 [-]: GETTABLE  R6 R6 K10    ; R6 := R6["getenv"]
   33 [-]: LOADK     R7 K14       ; R7 := "HTTPS"
   34 [-]: CALL      R6 2 2       ; R6 := R6(R7)
   35 [-]: GETGLOBAL R7 K0        ; R7 := require
   36 [-]: LOADK     R8 K15       ; R8 := "luci.model.network"
   37 [-]: CALL      R7 2 2       ; R7 := R7(R8)
   38 [-]: GETTABLE  R7 R7 K16    ; R7 := R7["init"]
   39 [-]: CALL      R7 1 2       ; R7 := R7()
   40 [-]: SELF      R8 R7 K17    ; R9 := R7; R8 := R7["get_network"]
   41 [-]: LOADK     R10 K18      ; R10 := "lan"
   42 [-]: CALL      R8 3 2       ; R8 := R8(R9 to R10)
   43 [-]: GETTABLE  R9 R2 K19    ; R9 := R2["ipaddr"]
   44 [-]: MOVE      R10 R4       ; R10 := R4
   45 [-]: CALL      R9 2 2       ; R9 := R9(R10)
   46 [-]: TEST      R9 1         ; if not R9 then goto 48 else goto 84
   47 [-]: JMP       36           ; PC += 36 (goto 84)
   48 [-]: EQ        1 R4 R1      ; if R4 ~= R1 then goto 50 else goto 84
   49 [-]: JMP       34           ; PC += 34 (goto 84)
   50 [-]: LOADK     R9 K20       ; R9 := "www."
   51 [-]: MOVE      R10 R1       ; R10 := R1
   52 [-]: CONCAT    R9 R9 R10    ; R9 := concat(R9 to R10)
   53 [-]: EQ        1 R4 R9      ; if R4 ~= R9 then goto 55 else goto 84
   54 [-]: JMP       29           ; PC += 29 (goto 84)
   55 [-]: TEST      R8 0         ; if R8 then goto 57 else goto 84
   56 [-]: JMP       27           ; PC += 27 (goto 84)
   57 [-]: SELF      R9 R8 K19    ; R10 := R8; R9 := R8["ipaddr"]
   58 [-]: CALL      R9 2 2       ; R9 := R9(R10)
   59 [-]: EQ        0 R3 R9      ; if R3 == R9 then goto 61 else goto 84
   60 [-]: JMP       23           ; PC += 23 (goto 84)
   61 [-]: LOADNIL   R9 R9        ; R9 := nil
   62 [-]: EQ        0 R6 K21     ; if R6 == "on" then goto 64 else goto 70
   63 [-]: JMP       6            ; PC += 6 (goto 70)
   64 [-]: LOADK     R10 K22      ; R10 := "https://"
   65 [-]: MOVE      R11 R1       ; R11 := R1
   66 [-]: MOVE      R12 R5       ; R12 := R5
   67 [-]: LOADK     R13 K23      ; R13 := "\r\n\r\n"
   68 [-]: CONCAT    R9 R10 R13   ; R9 := concat(R10 to R13)
   69 [-]: JMP       5            ; PC += 5 (goto 75)
   70 [-]: LOADK     R10 K24      ; R10 := "http://"
   71 [-]: MOVE      R11 R1       ; R11 := R1
   72 [-]: MOVE      R12 R5       ; R12 := R5
   73 [-]: LOADK     R13 K23      ; R13 := "\r\n\r\n"
   74 [-]: CONCAT    R9 R10 R13   ; R9 := concat(R10 to R13)
   75 [-]: GETGLOBAL R10 K8       ; R10 := luci
   76 [-]: GETTABLE  R10 R10 K9   ; R10 := R10["http"]
   77 [-]: GETTABLE  R10 R10 K25  ; R10 := R10["redirect"]
   78 [-]: MOVE      R11 R9       ; R11 := R9
   79 [-]: LOADK     R12 K26      ; R12 := Unknown_Type_Error
   80 [-]: LOADK     R13 K27      ; R13 := "Temporary Redirect"
   81 [-]: CALL      R10 4 1      ;  := R10(R11 to R13)
   82 [-]: LOADBOOL  R10 1 0      ; R10 := true
   83 [-]: RETURN    R10 2        ; return R10
   84 [-]: LOADBOOL  R2 0 0       ; R2 := false
   85 [-]: RETURN    R2 2         ; return R2
   86 [-]: RETURN    R0 1         ; return 


; Function:        0_18
; Defined at line: 397
; #Upvalues:       0
; #Parameters:     0
; Is_vararg:       0
; Max Stack Size:  14

    0 [-]: GETGLOBAL R0 K0        ; R0 := require
    1 [-]: LOADK     R1 K1        ; R1 := "luci.model.uci"
    2 [-]: CALL      R0 2 2       ; R0 := R0(R1)
    3 [-]: GETTABLE  R0 R0 K2     ; R0 := R0["cursor"]
    4 [-]: CALL      R0 1 2       ; R0 := R0()
    5 [-]: GETGLOBAL R1 K3        ; R1 := luci
    6 [-]: GETTABLE  R1 R1 K4     ; R1 := R1["http"]
    7 [-]: GETTABLE  R1 R1 K5     ; R1 := R1["getenv"]
    8 [-]: LOADK     R2 K6        ; R2 := "HTTPS"
    9 [-]: CALL      R1 2 2       ; R1 := R1(R2)
   10 [-]: SELF      R2 R0 K7     ; R3 := R0; R2 := R0["get"]
   11 [-]: LOADK     R4 K8        ; R4 := "uhttpd"
   12 [-]: LOADK     R5 K9        ; R5 := "main"
   13 [-]: LOADK     R6 K10       ; R6 := "force_https"
   14 [-]: CALL      R2 5 2       ; R2 := R2(R3 to R6)
   15 [-]: EQ        0 R2 K11     ; if R2 == "1" then goto 17 else goto 65
   16 [-]: JMP       48           ; PC += 48 (goto 65)
   17 [-]: EQ        1 R1 K12     ; if R1 ~= "on" then goto 19 else goto 65
   18 [-]: JMP       46           ; PC += 46 (goto 65)
   19 [-]: GETGLOBAL R3 K3        ; R3 := luci
   20 [-]: GETTABLE  R3 R3 K4     ; R3 := R3["http"]
   21 [-]: GETTABLE  R3 R3 K5     ; R3 := R3["getenv"]
   22 [-]: LOADK     R4 K13       ; R4 := "HTTP_HOST"
   23 [-]: CALL      R3 2 2       ; R3 := R3(R4)
   24 [-]: GETGLOBAL R4 K3        ; R4 := luci
   25 [-]: GETTABLE  R4 R4 K4     ; R4 := R4["http"]
   26 [-]: GETTABLE  R4 R4 K5     ; R4 := R4["getenv"]
   27 [-]: LOADK     R5 K14       ; R5 := "REQUEST_URI"
   28 [-]: CALL      R4 2 2       ; R4 := R4(R5)
   29 [-]: SELF      R5 R0 K7     ; R6 := R0; R5 := R0["get"]
   30 [-]: LOADK     R7 K8        ; R7 := "uhttpd"
   31 [-]: LOADK     R8 K9        ; R8 := "main"
   32 [-]: LOADK     R9 K15       ; R9 := "listen_https"
   33 [-]: CALL      R5 5 2       ; R5 := R5(R6 to R9)
   34 [-]: GETGLOBAL R6 K16       ; R6 := string
   35 [-]: GETTABLE  R6 R6 K17    ; R6 := R6["sub"]
   36 [-]: GETTABLE  R7 R5 K18    ; R7 := R5[Unknown_Type_Error]
   37 [-]: LOADK     R8 K19       ; R8 := Unknown_Type_Error
   38 [-]: LOADK     R9 K20       ; R9 := Unknown_Type_Error
   39 [-]: CALL      R6 4 2       ; R6 := R6(R7 to R9)
   40 [-]: LOADNIL   R7 R7        ; R7 := nil
   41 [-]: EQ        1 R6 K21     ; if R6 ~= "443" then goto 43 else goto 51
   42 [-]: JMP       8            ; PC += 8 (goto 51)
   43 [-]: LOADK     R8 K22       ; R8 := "https://"
   44 [-]: MOVE      R9 R3        ; R9 := R3
   45 [-]: LOADK     R10 K23      ; R10 := ":"
   46 [-]: MOVE      R11 R6       ; R11 := R6
   47 [-]: MOVE      R12 R4       ; R12 := R4
   48 [-]: LOADK     R13 K24      ; R13 := "\r\n\r\n"
   49 [-]: CONCAT    R7 R8 R13    ; R7 := concat(R8 to R13)
   50 [-]: JMP       5            ; PC += 5 (goto 56)
   51 [-]: LOADK     R8 K22       ; R8 := "https://"
   52 [-]: MOVE      R9 R3        ; R9 := R3
   53 [-]: MOVE      R10 R4       ; R10 := R4
   54 [-]: LOADK     R11 K24      ; R11 := "\r\n\r\n"
   55 [-]: CONCAT    R7 R8 R11    ; R7 := concat(R8 to R11)
   56 [-]: GETGLOBAL R8 K3        ; R8 := luci
   57 [-]: GETTABLE  R8 R8 K4     ; R8 := R8["http"]
   58 [-]: GETTABLE  R8 R8 K25    ; R8 := R8["redirect"]
   59 [-]: MOVE      R9 R7        ; R9 := R7
   60 [-]: LOADK     R10 K26      ; R10 := Unknown_Type_Error
   61 [-]: LOADK     R11 K27      ; R11 := "Temporary Redirect"
   62 [-]: CALL      R8 4 1       ;  := R8(R9 to R11)
   63 [-]: LOADBOOL  R8 1 0       ; R8 := true
   64 [-]: RETURN    R8 2         ; return R8
   65 [-]: LOADBOOL  R3 0 0       ; R3 := false
   66 [-]: RETURN    R3 2         ; return R3
   67 [-]: RETURN    R0 1         ; return 


; Function:        0_19
; Defined at line: 423
; #Upvalues:       12
; #Parameters:     1
; Is_vararg:       0
; Max Stack Size:  35

    0 [-]: GETGLOBAL R1 K0        ; R1 := context
    1 [-]: SETTABLE  R1 K1 R0     ; R1["path"] := R0
    2 [-]: GETGLOBAL R2 K2        ; R2 := require
    3 [-]: LOADK     R3 K3        ; R3 := "luci.config"
    4 [-]: CALL      R2 2 2       ; R2 := R2(R3)
    5 [-]: GETGLOBAL R3 K4        ; R3 := assert
    6 [-]: GETTABLE  R4 R2 K5     ; R4 := R2["main"]
    7 [-]: LOADK     R5 K6        ; R5 := "/etc/config/luci seems to be corrupt, unable to find section \'main\'"
    8 [-]: CALL      R3 3 1       ;  := R3(R4 to R5)
    9 [-]: GETGLOBAL R3 K2        ; R3 := require
   10 [-]: LOADK     R4 K7        ; R4 := "luci.i18n"
   11 [-]: CALL      R3 2 2       ; R3 := R3(R4)
   12 [-]: GETTABLE  R4 R2 K5     ; R4 := R2["main"]
   13 [-]: GETTABLE  R4 R4 K8     ; R4 := R4["lang"]
   14 [-]: TEST      R4 1         ; if not R4 then goto 16 else goto 17
   15 [-]: JMP       1            ; PC += 1 (goto 17)
   16 [-]: LOADK     R4 K9        ; R4 := "auto"
   17 [-]: EQ        0 R4 K9      ; if R4 == "auto" then goto 19 else goto 95
   18 [-]: JMP       76           ; PC += 76 (goto 95)
   19 [-]: GETUPVAL  R5 U0        ; R5 := U0
   20 [-]: GETTABLE  R5 R5 K10    ; R5 := R5["getenv"]
   21 [-]: LOADK     R6 K11       ; R6 := "HTTP_ACCEPT_LANGUAGE"
   22 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   23 [-]: TEST      R5 1         ; if not R5 then goto 25 else goto 26
   24 [-]: JMP       1            ; PC += 1 (goto 26)
   25 [-]: LOADK     R5 K12       ; R5 := ""
   26 [-]: SELF      R6 R5 K13    ; R7 := R5; R6 := R5["gmatch"]
   27 [-]: LOADK     R8 K14       ; R8 := "[%w_-]+"
   28 [-]: CALL      R6 3 4       ; R6 to R8 := R6(R7 to R8)
   29 [-]: JMP       63           ; PC += 63 (goto 93)
   30 [-]: SELF      R10 R9 K15   ; R11 := R9; R10 := R9["match"]
   31 [-]: LOADK     R12 K16      ; R12 := "^([a-z][a-z])[_-]([a-zA-Z][a-zA-Z])$"
   32 [-]: CALL      R10 3 3      ; R10 to R11 := R10(R11 to R12)
   33 [-]: TEST      R10 0        ; if R10 then goto 35 else goto 66
   34 [-]: JMP       31           ; PC += 31 (goto 66)
   35 [-]: TEST      R11 0        ; if R11 then goto 37 else goto 66
   36 [-]: JMP       29           ; PC += 29 (goto 66)
   37 [-]: NEWTABLE  R12 1 0      ; R12 := {} (size = 1,0)
   38 [-]: MOVE      R13 R10      ; R13 := R10
   39 [-]: SELF      R14 R11 K18  ; R15 := R11; R14 := R11["lower"]
   40 [-]: CALL      R14 2 0      ; R14 to top := R14(R15)
   41 [-]: SETLIST   R12 0 1      ; R12[0] to R12[top] := R13 to top ; R(a)[(c-1)*FPF+i] := R(a+i), 1 <= i <= b, a=12, b=0, c=1, FPF=50
   42 [-]: MOD       R12 K17 R12  ; R12 := "%s_%s" % R12
   43 [-]: GETTABLE  R13 R2 K19   ; R13 := R2["languages"]
   44 [-]: GETTABLE  R13 R13 R12  ; R13 := R13[R12]
   45 [-]: TEST      R13 0        ; if R13 then goto 47 else goto 50
   46 [-]: JMP       3            ; PC += 3 (goto 50)
   47 [-]: MOVE      R4 R12       ; R4 := R12
   48 [-]: JMP       46           ; PC += 46 (goto 95)
   49 [-]: JMP       43           ; PC += 43 (goto 93)
   50 [-]: GETTABLE  R13 R2 K19   ; R13 := R2["languages"]
   51 [-]: GETTABLE  R13 R13 R10  ; R13 := R13[R10]
   52 [-]: TEST      R13 0        ; if R13 then goto 54 else goto 57
   53 [-]: JMP       3            ; PC += 3 (goto 57)
   54 [-]: MOVE      R4 R10       ; R4 := R10
   55 [-]: JMP       39           ; PC += 39 (goto 95)
   56 [-]: JMP       36           ; PC += 36 (goto 93)
   57 [-]: EQ        0 R12 K20    ; if R12 == "zh_hk" then goto 59 else goto 93
   58 [-]: JMP       34           ; PC += 34 (goto 93)
   59 [-]: GETTABLE  R13 R2 K19   ; R13 := R2["languages"]
   60 [-]: GETTABLE  R13 R13 K21  ; R13 := R13["zh_tw"]
   61 [-]: TEST      R13 0        ; if R13 then goto 63 else goto 93
   62 [-]: JMP       30           ; PC += 30 (goto 93)
   63 [-]: LOADK     R4 K21       ; R4 := "zh_tw"
   64 [-]: JMP       30           ; PC += 30 (goto 95)
   65 [-]: JMP       27           ; PC += 27 (goto 93)
   66 [-]: GETTABLE  R12 R2 K19   ; R12 := R2["languages"]
   67 [-]: GETTABLE  R12 R12 R9   ; R12 := R12[R9]
   68 [-]: TEST      R12 0        ; if R12 then goto 70 else goto 73
   69 [-]: JMP       3            ; PC += 3 (goto 73)
   70 [-]: MOVE      R4 R9        ; R4 := R9
   71 [-]: JMP       23           ; PC += 23 (goto 95)
   72 [-]: JMP       20           ; PC += 20 (goto 93)
   73 [-]: EQ        0 R9 K22     ; if R9 == "zh" then goto 75 else goto 93
   74 [-]: JMP       18           ; PC += 18 (goto 93)
   75 [-]: GETUPVAL  R12 U1       ; R12 := U1
   76 [-]: GETTABLE  R12 R12 K23  ; R12 := R12["exec"]
   77 [-]: LOADK     R13 K24      ; R13 := "bdinfo country"
   78 [-]: CALL      R12 2 2      ; R12 := R12(R13)
   79 [-]: EQ        0 R12 K25    ; if R12 == "CN" then goto 81 else goto 87
   80 [-]: JMP       6            ; PC += 6 (goto 87)
   81 [-]: GETTABLE  R13 R2 K19   ; R13 := R2["languages"]
   82 [-]: GETTABLE  R13 R13 K26  ; R13 := R13["zh_cn"]
   83 [-]: TEST      R13 0        ; if R13 then goto 85 else goto 87
   84 [-]: JMP       2            ; PC += 2 (goto 87)
   85 [-]: LOADK     R4 K26       ; R4 := "zh_cn"
   86 [-]: JMP       8            ; PC += 8 (goto 95)
   87 [-]: GETTABLE  R13 R2 K19   ; R13 := R2["languages"]
   88 [-]: GETTABLE  R13 R13 K21  ; R13 := R13["zh_tw"]
   89 [-]: TEST      R13 0        ; if R13 then goto 91 else goto 95
   90 [-]: JMP       4            ; PC += 4 (goto 95)
   91 [-]: LOADK     R4 K21       ; R4 := "zh_tw"
   92 [-]: JMP       2            ; PC += 2 (goto 95)
   93 [-]: TFORLOOP  R6 1         ; R9 := R6(R7,R8); if R9 ~= nil then R8 := R9 else goto 95
   94 [-]: JMP       -65          ; PC += -65 (goto 30)
   95 [-]: EQ        0 R4 K9      ; if R4 == "auto" then goto 97 else goto 98
   96 [-]: JMP       1            ; PC += 1 (goto 98)
   97 [-]: GETTABLE  R4 R3 K27    ; R4 := R3["default"]
   98 [-]: GETTABLE  R5 R1 K28    ; R5 := R1["tree"]
   99 [-]: LOADNIL   R6 R6        ; R6 := nil
  100 [-]: TEST      R5 1         ; if not R5 then goto 102 else goto 105
  101 [-]: JMP       3            ; PC += 3 (goto 105)
  102 [-]: GETGLOBAL R7 K29       ; R7 := createtree
  103 [-]: CALL      R7 1 2       ; R7 := R7()
  104 [-]: MOVE      R5 R7        ; R5 := R7
  105 [-]: NEWTABLE  R7 0 0       ; R7 := {} (size = 0,0)
  106 [-]: NEWTABLE  R8 0 0       ; R8 := {} (size = 0,0)
  107 [-]: SETTABLE  R1 K30 R8    ; R1["args"] := R8
  108 [-]: GETTABLE  R9 R1 K31    ; R9 := R1["requestargs"]
  109 [-]: TEST      R9 1         ; if not R9 then goto 111 else goto 112
  110 [-]: JMP       1            ; PC += 1 (goto 112)
  111 [-]: MOVE      R9 R8        ; R9 := R8
  112 [-]: SETTABLE  R1 K31 R9    ; R1["requestargs"] := R9
  113 [-]: LOADNIL   R9 R9        ; R9 := nil
  114 [-]: NEWTABLE  R10 0 0      ; R10 := {} (size = 0,0)
  115 [-]: NEWTABLE  R11 0 0      ; R11 := {} (size = 0,0)
  116 [-]: GETGLOBAL R12 K32      ; R12 := ipairs
  117 [-]: MOVE      R13 R0       ; R13 := R0
  118 [-]: CALL      R12 2 4      ; R12 to R14 := R12(R13)
  119 [-]: JMP       21           ; PC += 21 (goto 141)
  120 [-]: LEN       R17 R10      ; R17 := #R10
  121 [-]: ADD       R17 R17 K33  ; R17 := R17 + Unknown_Type_Error
  122 [-]: SETTABLE  R10 R17 R16  ; R10[R17] := R16
  123 [-]: LEN       R17 R11      ; R17 := #R11
  124 [-]: ADD       R17 R17 K33  ; R17 := R17 + Unknown_Type_Error
  125 [-]: SETTABLE  R11 R17 R16  ; R11[R17] := R16
  126 [-]: GETTABLE  R17 R5 K34   ; R17 := R5["nodes"]
  127 [-]: GETTABLE  R5 R17 R16   ; R5 := R17[R16]
  128 [-]: MOVE      R9 R15       ; R9 := R15
  129 [-]: TEST      R5 1         ; if not R5 then goto 131 else goto 132
  130 [-]: JMP       1            ; PC += 1 (goto 132)
  131 [-]: JMP       11           ; PC += 11 (goto 143)
  132 [-]: GETUPVAL  R17 U1       ; R17 := U1
  133 [-]: GETTABLE  R17 R17 K35  ; R17 := R17["update"]
  134 [-]: MOVE      R18 R7       ; R18 := R7
  135 [-]: MOVE      R19 R5       ; R19 := R5
  136 [-]: CALL      R17 3 1      ;  := R17(R18 to R19)
  137 [-]: GETTABLE  R17 R5 K36   ; R17 := R5["leaf"]
  138 [-]: TEST      R17 0        ; if R17 then goto 140 else goto 141
  139 [-]: JMP       1            ; PC += 1 (goto 141)
  140 [-]: JMP       2            ; PC += 2 (goto 143)
  141 [-]: TFORLOOP  R12 2        ; R15 to R16 := R12(R13,R14); if R15 ~= nil then R14 := R15 else goto 143
  142 [-]: JMP       -23          ; PC += -23 (goto 120)
  143 [-]: GETTABLE  R12 R7 K37   ; R12 := R7["httponly"]
  144 [-]: TEST      R12 1        ; if not R12 then goto 146 else goto 151
  145 [-]: JMP       5            ; PC += 5 (goto 151)
  146 [-]: GETGLOBAL R12 K38      ; R12 := tls_redirect_check
  147 [-]: CALL      R12 1 2      ; R12 := R12()
  148 [-]: TEST      R12 0        ; if R12 then goto 150 else goto 151
  149 [-]: JMP       1            ; PC += 1 (goto 151)
  150 [-]: RETURN    R0 1         ; return 
  151 [-]: TEST      R5 0         ; if R5 then goto 153 else goto 169
  152 [-]: JMP       16           ; PC += 16 (goto 169)
  153 [-]: GETTABLE  R12 R5 K36   ; R12 := R5["leaf"]
  154 [-]: TEST      R12 0        ; if R12 then goto 156 else goto 169
  155 [-]: JMP       13           ; PC += 13 (goto 169)
  156 [-]: ADD       R12 R9 K33   ; R12 := R9 + Unknown_Type_Error
  157 [-]: LEN       R13 R0       ; R13 := #R0
  158 [-]: LOADK     R14 K33      ; R14 := Unknown_Type_Error
  159 [-]: FORPREP   R12 8        ; R12 -= R14; pc += 8 (goto 168)
  160 [-]: LEN       R16 R8       ; R16 := #R8
  161 [-]: ADD       R16 R16 K33  ; R16 := R16 + Unknown_Type_Error
  162 [-]: GETTABLE  R17 R0 R15   ; R17 := R0[R15]
  163 [-]: SETTABLE  R8 R16 R17   ; R8[R16] := R17
  164 [-]: LEN       R16 R11      ; R16 := #R11
  165 [-]: ADD       R16 R16 K33  ; R16 := R16 + Unknown_Type_Error
  166 [-]: GETTABLE  R17 R0 R15   ; R17 := R0[R15]
  167 [-]: SETTABLE  R11 R16 R17  ; R11[R16] := R17
  168 [-]: FORLOOP   R12 -9       ; R12 += R14; if R12 <= R13 then R15 := R12; PC += -9 , goto 160 end
  169 [-]: GETTABLE  R12 R1 K39   ; R12 := R1["requestpath"]
  170 [-]: TEST      R12 1        ; if not R12 then goto 172 else goto 173
  171 [-]: JMP       1            ; PC += 1 (goto 173)
  172 [-]: MOVE      R12 R11      ; R12 := R11
  173 [-]: SETTABLE  R1 K39 R12   ; R1["requestpath"] := R12
  174 [-]: SETTABLE  R1 K1 R10    ; R1["path"] := R10
  175 [-]: GETTABLE  R12 R7 K40   ; R12 := R7["i18n"]
  176 [-]: TEST      R12 0        ; if R12 then goto 178 else goto 181
  177 [-]: JMP       3            ; PC += 3 (goto 181)
  178 [-]: GETTABLE  R12 R3 K41   ; R12 := R3["loadc"]
  179 [-]: GETTABLE  R13 R7 K40   ; R13 := R7["i18n"]
  180 [-]: CALL      R12 2 1      ;  := R12(R13)
  181 [-]: GETTABLE  R12 R7 K8    ; R12 := R7["lang"]
  182 [-]: TEST      R12 0        ; if R12 then goto 184 else goto 188
  183 [-]: JMP       4            ; PC += 4 (goto 188)
  184 [-]: GETTABLE  R12 R3 K42   ; R12 := R3["setlanguage"]
  185 [-]: GETTABLE  R13 R7 K8    ; R13 := R7["lang"]
  186 [-]: CALL      R12 2 1      ;  := R12(R13)
  187 [-]: JMP       3            ; PC += 3 (goto 191)
  188 [-]: GETTABLE  R12 R3 K42   ; R12 := R3["setlanguage"]
  189 [-]: MOVE      R13 R4       ; R13 := R4
  190 [-]: CALL      R12 2 1      ;  := R12(R13)
  191 [-]: TEST      R5 0         ; if R5 then goto 193 else goto 196
  192 [-]: JMP       3            ; PC += 3 (goto 196)
  193 [-]: GETTABLE  R12 R5 K43   ; R12 := R5["index"]
  194 [-]: TEST      R12 1        ; if not R12 then goto 196 else goto 199
  195 [-]: JMP       3            ; PC += 3 (goto 199)
  196 [-]: GETTABLE  R12 R7 K44   ; R12 := R7["notemplate"]
  197 [-]: TEST      R12 1        ; if not R12 then goto 199 else goto 299
  198 [-]: JMP       100          ; PC += 100 (goto 299)
  199 [-]: GETGLOBAL R12 K2       ; R12 := require
  200 [-]: LOADK     R13 K45      ; R13 := "luci.template"
  201 [-]: CALL      R12 2 2      ; R12 := R12(R13)
  202 [-]: GETTABLE  R13 R7 K46   ; R13 := R7["mediaurlbase"]
  203 [-]: TEST      R13 1        ; if not R13 then goto 205 else goto 209
  204 [-]: JMP       4            ; PC += 4 (goto 209)
  205 [-]: GETGLOBAL R13 K47      ; R13 := luci
  206 [-]: GETTABLE  R13 R13 K48  ; R13 := R13["config"]
  207 [-]: GETTABLE  R13 R13 K5   ; R13 := R13["main"]
  208 [-]: GETTABLE  R13 R13 K46  ; R13 := R13["mediaurlbase"]
  209 [-]: GETGLOBAL R14 K49      ; R14 := pcall
  210 [-]: GETTABLE  R15 R12 K50  ; R15 := R12["Template"]
  211 [-]: GETUPVAL  R16 U2       ; R16 := U2
  212 [-]: GETTABLE  R16 R16 K52  ; R16 := R16["basename"]
  213 [-]: MOVE      R17 R13      ; R17 := R13
  214 [-]: CALL      R16 2 2      ; R16 := R16(R17)
  215 [-]: MOD       R16 K51 R16  ; R16 := "themes/%s/header" % R16
  216 [-]: CALL      R14 3 2      ; R14 := R14(R15 to R16)
  217 [-]: TEST      R14 1        ; if not R14 then goto 219 else goto 249
  218 [-]: JMP       30           ; PC += 30 (goto 249)
  219 [-]: LOADNIL   R13 R13      ; R13 := nil
  220 [-]: GETGLOBAL R14 K53      ; R14 := pairs
  221 [-]: GETGLOBAL R15 K47      ; R15 := luci
  222 [-]: GETTABLE  R15 R15 K48  ; R15 := R15["config"]
  223 [-]: GETTABLE  R15 R15 K54  ; R15 := R15["themes"]
  224 [-]: CALL      R14 2 4      ; R14 to R16 := R14(R15)
  225 [-]: JMP       17           ; PC += 17 (goto 243)
  226 [-]: SELF      R19 R17 K55  ; R20 := R17; R19 := R17["sub"]
  227 [-]: LOADK     R21 K33      ; R21 := Unknown_Type_Error
  228 [-]: LOADK     R22 K33      ; R22 := Unknown_Type_Error
  229 [-]: CALL      R19 4 2      ; R19 := R19(R20 to R22)
  230 [-]: EQ        1 R19 K56    ; if R19 ~= "." then goto 232 else goto 243
  231 [-]: JMP       11           ; PC += 11 (goto 243)
  232 [-]: GETGLOBAL R19 K49      ; R19 := pcall
  233 [-]: GETTABLE  R20 R12 K50  ; R20 := R12["Template"]
  234 [-]: GETUPVAL  R21 U2       ; R21 := U2
  235 [-]: GETTABLE  R21 R21 K52  ; R21 := R21["basename"]
  236 [-]: MOVE      R22 R18      ; R22 := R18
  237 [-]: CALL      R21 2 2      ; R21 := R21(R22)
  238 [-]: MOD       R21 K51 R21  ; R21 := "themes/%s/header" % R21
  239 [-]: CALL      R19 3 2      ; R19 := R19(R20 to R21)
  240 [-]: TEST      R19 0        ; if R19 then goto 242 else goto 243
  241 [-]: JMP       1            ; PC += 1 (goto 243)
  242 [-]: MOVE      R13 R18      ; R13 := R18
  243 [-]: TFORLOOP  R14 2        ; R17 to R18 := R14(R15,R16); if R17 ~= nil then R16 := R17 else goto 245
  244 [-]: JMP       -19          ; PC += -19 (goto 226)
  245 [-]: GETGLOBAL R14 K4       ; R14 := assert
  246 [-]: MOVE      R15 R13      ; R15 := R13
  247 [-]: LOADK     R16 K57      ; R16 := "No valid theme found"
  248 [-]: CALL      R14 3 1      ;  := R14(R15 to R16)
  249 [-]: CLOSURE   R14 0        ; R14 := closure(Function #0_19_0)
  250 [-]: GETUPVAL  R0 U1        ; R0 := U1
  251 [-]: GETTABLE  R15 R12 K0   ; R15 := R12["context"]
  252 [-]: GETGLOBAL R16 K59      ; R16 := setmetatable
  253 [-]: NEWTABLE  R17 0 13     ; R17 := {} (size = 0,13)
  254 [-]: GETUPVAL  R18 U0       ; R18 := U0
  255 [-]: GETTABLE  R18 R18 K60  ; R18 := R18["write"]
  256 [-]: SETTABLE  R17 K60 R18  ; R17["write"] := R18
  257 [-]: CLOSURE   R18 1        ; R18 := closure(Function #0_19_1)
  258 [-]: MOVE      R0 R12       ; R0 := R12
  259 [-]: SETTABLE  R17 K61 R18  ; R17["include"] := R18
  260 [-]: GETTABLE  R18 R3 K62   ; R18 := R3["translate"]
  261 [-]: SETTABLE  R17 K62 R18  ; R17["translate"] := R18
  262 [-]: GETTABLE  R18 R3 K63   ; R18 := R3["translatef"]
  263 [-]: SETTABLE  R17 K63 R18  ; R17["translatef"] := R18
  264 [-]: CLOSURE   R18 2        ; R18 := closure(Function #0_19_2)
  265 [-]: MOVE      R0 R12       ; R0 := R12
  266 [-]: SETTABLE  R17 K64 R18  ; R17["export"] := R18
  267 [-]: GETUPVAL  R18 U1       ; R18 := U1
  268 [-]: GETTABLE  R18 R18 K65  ; R18 := R18["striptags"]
  269 [-]: SETTABLE  R17 K65 R18  ; R17["striptags"] := R18
  270 [-]: GETUPVAL  R18 U1       ; R18 := U1
  271 [-]: GETTABLE  R18 R18 K66  ; R18 := R18["pcdata"]
  272 [-]: SETTABLE  R17 K66 R18  ; R17["pcdata"] := R18
  273 [-]: SETTABLE  R17 K67 R13  ; R17["media"] := R13
  274 [-]: GETUPVAL  R18 U2       ; R18 := U2
  275 [-]: GETTABLE  R18 R18 K52  ; R18 := R18["basename"]
  276 [-]: MOVE      R19 R13      ; R19 := R13
  277 [-]: CALL      R18 2 2      ; R18 := R18(R19)
  278 [-]: SETTABLE  R17 K68 R18  ; R17["theme"] := R18
  279 [-]: GETGLOBAL R18 K47      ; R18 := luci
  280 [-]: GETTABLE  R18 R18 K48  ; R18 := R18["config"]
  281 [-]: GETTABLE  R18 R18 K5   ; R18 := R18["main"]
  282 [-]: GETTABLE  R18 R18 K70  ; R18 := R18["resourcebase"]
  283 [-]: SETTABLE  R17 K69 R18  ; R17["resource"] := R18
  284 [-]: CLOSURE   R18 3        ; R18 := closure(Function #0_19_3)
  285 [-]: MOVE      R0 R14       ; R0 := R14
  286 [-]: SETTABLE  R17 K71 R18  ; R17["ifattr"] := R18
  287 [-]: CLOSURE   R18 4        ; R18 := closure(Function #0_19_4)
  288 [-]: MOVE      R0 R14       ; R0 := R14
  289 [-]: SETTABLE  R17 K72 R18  ; R17["attr"] := R18
  290 [-]: GETGLOBAL R18 K74      ; R18 := build_url
  291 [-]: SETTABLE  R17 K73 R18  ; R17["url"] := R18
  292 [-]: NEWTABLE  R18 0 1      ; R18 := {} (size = 0,1)
  293 [-]: CLOSURE   R19 5        ; R19 := closure(Function #0_19_5)
  294 [-]: MOVE      R0 R1        ; R0 := R1
  295 [-]: SETTABLE  R18 K75 R19  ; R18["__index"] := R19
  296 [-]: CALL      R16 3 2      ; R16 := R16(R17 to R18)
  297 [-]: SETTABLE  R15 K58 R16  ; R15["viewns"] := R16
  298 [-]: CLOSE     R12          ; close all upvalues in R12 to top
  299 [-]: GETTABLE  R12 R7 K76   ; R12 := R7["dependent"]
  300 [-]: EQ        0 R12 K77    ; if R12 == false then goto 302 else goto 303
  301 [-]: JMP       1            ; PC += 1 (goto 303)
  302 [-]: LOADBOOL  R12 0 1      ; R12 := false; goto 304
  303 [-]: LOADBOOL  R12 1 0      ; R12 := true
  304 [-]: SETTABLE  R7 K76 R12   ; R7["dependent"] := R12
  305 [-]: GETGLOBAL R12 K4       ; R12 := assert
  306 [-]: GETTABLE  R13 R7 K76   ; R13 := R7["dependent"]
  307 [-]: TEST      R13 0        ; if R13 then goto 309 else goto 313
  308 [-]: JMP       4            ; PC += 4 (goto 313)
  309 [-]: GETTABLE  R13 R7 K9    ; R13 := R7["auto"]
  310 [-]: NOT       R13 R13      ; R13 := not R13
  311 [-]: JMP       2            ; PC += 2 (goto 314)
  312 [-]: LOADBOOL  R13 0 1      ; R13 := false; goto 314
  313 [-]: LOADBOOL  R13 1 0      ; R13 := true
  314 [-]: LOADK     R14 K78      ; R14 := "Access Violation\nThe page at \'"
  315 [-]: GETGLOBAL R15 K79      ; R15 := table
  316 [-]: GETTABLE  R15 R15 K80  ; R15 := R15["concat"]
  317 [-]: MOVE      R16 R0       ; R16 := R0
  318 [-]: LOADK     R17 K81      ; R17 := "/"
  319 [-]: CALL      R15 3 2      ; R15 := R15(R16 to R17)
  320 [-]: LOADK     R16 K82      ; R16 := "/\' "
  321 [-]: LOADK     R17 K83      ; R17 := "has no parent node so the access to this location has been denied.\n"
  322 [-]: LOADK     R18 K84      ; R18 := "This is a software bug, please report this message at "
  323 [-]: LOADK     R19 K85      ; R19 := "https://github.com/openwrt/luci/issues"
  324 [-]: CONCAT    R14 R14 R19  ; R14 := concat(R14 to R19)
  325 [-]: CALL      R12 3 1      ;  := R12(R13 to R14)
  326 [-]: GETTABLE  R12 R7 K86   ; R12 := R7["sysauth"]
  327 [-]: TEST      R12 0        ; if R12 then goto 329 else goto 644
  328 [-]: JMP       315          ; PC += 315 (goto 644)
  329 [-]: GETTABLE  R12 R7 K87   ; R12 := R7["sysauth_authenticator"]
  330 [-]: LOADNIL   R13 R17      ; R13 to R17 := nil
  331 [-]: GETGLOBAL R18 K88      ; R18 := type
  332 [-]: MOVE      R19 R12      ; R19 := R12
  333 [-]: CALL      R18 2 2      ; R18 := R18(R19)
  334 [-]: EQ        0 R18 K89    ; if R18 == "string" then goto 336 else goto 342
  335 [-]: JMP       6            ; PC += 6 (goto 342)
  336 [-]: EQ        1 R12 K90    ; if R12 ~= "htmlauth" then goto 338 else goto 342
  337 [-]: JMP       4            ; PC += 4 (goto 342)
  338 [-]: GETGLOBAL R18 K91      ; R18 := error500
  339 [-]: MOD       R19 K92 R12  ; R19 := "Unsupported authenticator %q configured" % R12
  340 [-]: CALL      R18 2 1      ;  := R18(R19)
  341 [-]: RETURN    R0 1         ; return 
  342 [-]: GETGLOBAL R18 K88      ; R18 := type
  343 [-]: GETTABLE  R19 R7 K86   ; R19 := R7["sysauth"]
  344 [-]: CALL      R18 2 2      ; R18 := R18(R19)
  345 [-]: EQ        0 R18 K79    ; if R18 == "table" then goto 347 else goto 351
  346 [-]: JMP       4            ; PC += 4 (goto 351)
  347 [-]: LOADNIL   R18 R18      ; R18 := nil
  348 [-]: GETTABLE  R17 R7 K86   ; R17 := R7["sysauth"]
  349 [-]: MOVE      R16 R18      ; R16 := R18
  350 [-]: JMP       6            ; PC += 6 (goto 357)
  351 [-]: GETTABLE  R18 R7 K86   ; R18 := R7["sysauth"]
  352 [-]: NEWTABLE  R19 1 0      ; R19 := {} (size = 1,0)
  353 [-]: GETTABLE  R20 R7 K86   ; R20 := R7["sysauth"]
  354 [-]: SETLIST   R19 1 1      ; R19[0] := R20 ; R(a)[(c-1)*FPF+i] := R(a+i), 1 <= i <= b, a=19, b=1, c=1, FPF=50
  355 [-]: MOVE      R17 R19      ; R17 := R19
  356 [-]: MOVE      R16 R18      ; R16 := R18
  357 [-]: GETGLOBAL R18 K88      ; R18 := type
  358 [-]: MOVE      R19 R12      ; R19 := R12
  359 [-]: CALL      R18 2 2      ; R18 := R18(R19)
  360 [-]: EQ        0 R18 K93    ; if R18 == "function" then goto 362 else goto 371
  361 [-]: JMP       9            ; PC += 9 (goto 371)
  362 [-]: MOVE      R18 R12      ; R18 := R12
  363 [-]: GETUPVAL  R19 U3       ; R19 := U3
  364 [-]: GETTABLE  R19 R19 K94  ; R19 := R19["user"]
  365 [-]: GETTABLE  R19 R19 K95  ; R19 := R19["checkpasswd"]
  366 [-]: MOVE      R20 R17      ; R20 := R17
  367 [-]: CALL      R18 3 3      ; R18 to R19 := R18(R19 to R20)
  368 [-]: MOVE      R14 R19      ; R14 := R19
  369 [-]: MOVE      R13 R18      ; R13 := R18
  370 [-]: JMP       5            ; PC += 5 (goto 376)
  371 [-]: GETUPVAL  R18 U0       ; R18 := U0
  372 [-]: GETTABLE  R18 R18 K96  ; R18 := R18["getcookie"]
  373 [-]: LOADK     R19 K86      ; R19 := "sysauth"
  374 [-]: CALL      R18 2 2      ; R18 := R18(R19)
  375 [-]: MOVE      R14 R18      ; R14 := R18
  376 [-]: GETUPVAL  R18 U4       ; R18 := U4
  377 [-]: MOVE      R19 R14      ; R19 := R14
  378 [-]: MOVE      R20 R17      ; R20 := R17
  379 [-]: CALL      R18 3 3      ; R18 to R19 := R18(R19 to R20)
  380 [-]: MOVE      R15 R19      ; R15 := R19
  381 [-]: MOVE      R14 R18      ; R14 := R18
  382 [-]: TEST      R14 0        ; if R14 then goto 384 else goto 386
  383 [-]: JMP       2            ; PC += 2 (goto 386)
  384 [-]: TEST      R15 1        ; if not R15 then goto 386 else goto 612
  385 [-]: JMP       226          ; PC += 226 (goto 612)
  386 [-]: EQ        0 R12 K90    ; if R12 == "htmlauth" then goto 388 else goto 612
  387 [-]: JMP       224          ; PC += 224 (goto 612)
  388 [-]: GETUPVAL  R18 U0       ; R18 := U0
  389 [-]: GETTABLE  R18 R18 K10  ; R18 := R18["getenv"]
  390 [-]: LOADK     R19 K97      ; R19 := "HTTP_AUTH_USER"
  391 [-]: CALL      R18 2 2      ; R18 := R18(R19)
  392 [-]: GETUPVAL  R19 U0       ; R19 := U0
  393 [-]: GETTABLE  R19 R19 K10  ; R19 := R19["getenv"]
  394 [-]: LOADK     R20 K98      ; R20 := "HTTP_AUTH_PASS"
  395 [-]: CALL      R19 2 2      ; R19 := R19(R20)
  396 [-]: EQ        0 R18 K99    ; if R18 == nil then goto 398 else goto 410
  397 [-]: JMP       12           ; PC += 12 (goto 410)
  398 [-]: EQ        0 R19 K99    ; if R19 == nil then goto 400 else goto 410
  399 [-]: JMP       10           ; PC += 10 (goto 410)
  400 [-]: GETUPVAL  R20 U0       ; R20 := U0
  401 [-]: GETTABLE  R20 R20 K100 ; R20 := R20["formvalue"]
  402 [-]: LOADK     R21 K101     ; R21 := "luci_username"
  403 [-]: CALL      R20 2 2      ; R20 := R20(R21)
  404 [-]: MOVE      R18 R20      ; R18 := R20
  405 [-]: GETUPVAL  R20 U0       ; R20 := U0
  406 [-]: GETTABLE  R20 R20 K100 ; R20 := R20["formvalue"]
  407 [-]: LOADK     R21 K102     ; R21 := "luci_password"
  408 [-]: CALL      R20 2 2      ; R20 := R20(R21)
  409 [-]: MOVE      R19 R20      ; R19 := R20
  410 [-]: TEST      R18 0        ; if R18 then goto 412 else goto 418
  411 [-]: JMP       6            ; PC += 6 (goto 418)
  412 [-]: GETUPVAL  R20 U5       ; R20 := U5
  413 [-]: MOVE      R21 R18      ; R21 := R18
  414 [-]: CALL      R20 2 2      ; R20 := R20(R21)
  415 [-]: TEST      R20 1        ; if not R20 then goto 417 else goto 418
  416 [-]: JMP       1            ; PC += 1 (goto 418)
  417 [-]: LOADNIL   R18 R18      ; R18 := nil
  418 [-]: TEST      R18 0        ; if R18 then goto 420 else goto 432
  419 [-]: JMP       12           ; PC += 12 (goto 432)
  420 [-]: GETUPVAL  R20 U6       ; R20 := U6
  421 [-]: MOVE      R21 R18      ; R21 := R18
  422 [-]: CALL      R20 2 2      ; R20 := R20(R21)
  423 [-]: TEST      R20 1        ; if not R20 then goto 425 else goto 432
  424 [-]: JMP       7            ; PC += 7 (goto 432)
  425 [-]: GETUPVAL  R20 U7       ; R20 := U7
  426 [-]: MOVE      R21 R18      ; R21 := R18
  427 [-]: MOVE      R22 R19      ; R22 := R19
  428 [-]: MOVE      R23 R17      ; R23 := R17
  429 [-]: CALL      R20 4 3      ; R20 to R21 := R20(R21 to R23)
  430 [-]: MOVE      R15 R21      ; R15 := R21
  431 [-]: MOVE      R14 R20      ; R14 := R20
  432 [-]: TEST      R14 1        ; if not R14 then goto 434 else goto 471
  433 [-]: JMP       37           ; PC += 37 (goto 471)
  434 [-]: GETGLOBAL R20 K2       ; R20 := require
  435 [-]: LOADK     R21 K45      ; R21 := "luci.template"
  436 [-]: CALL      R20 2 2      ; R20 := R20(R21)
  437 [-]: GETGLOBAL R21 K0       ; R21 := context
  438 [-]: NEWTABLE  R22 0 0      ; R22 := {} (size = 0,0)
  439 [-]: SETTABLE  R21 K1 R22   ; R21["path"] := R22
  440 [-]: GETGLOBAL R21 K103     ; R21 := host_redirect_check
  441 [-]: CALL      R21 1 2      ; R21 := R21()
  442 [-]: TEST      R21 0        ; if R21 then goto 444 else goto 445
  443 [-]: JMP       1            ; PC += 1 (goto 445)
  444 [-]: RETURN    R0 1         ; return 
  445 [-]: GETUPVAL  R21 U0       ; R21 := U0
  446 [-]: GETTABLE  R21 R21 K104 ; R21 := R21["status"]
  447 [-]: LOADK     R22 K105     ; R22 := Unknown_Type_Error
  448 [-]: LOADK     R23 K106     ; R23 := "Forbidden"
  449 [-]: CALL      R21 3 1      ;  := R21(R22 to R23)
  450 [-]: GETTABLE  R21 R20 K107 ; R21 := R20["render"]
  451 [-]: GETTABLE  R22 R7 K108  ; R22 := R7["sysauth_template"]
  452 [-]: TEST      R22 1        ; if not R22 then goto 454 else goto 455
  453 [-]: JMP       1            ; PC += 1 (goto 455)
  454 [-]: LOADK     R22 K86      ; R22 := "sysauth"
  455 [-]: NEWTABLE  R23 0 5      ; R23 := {} (size = 0,5)
  456 [-]: SETTABLE  R23 K109 R16 ; R23["duser"] := R16
  457 [-]: SETTABLE  R23 K110 R18 ; R23["fuser"] := R18
  458 [-]: GETUPVAL  R24 U8       ; R24 := U8
  459 [-]: MOVE      R25 R18      ; R25 := R18
  460 [-]: CALL      R24 2 2      ; R24 := R24(R25)
  461 [-]: SETTABLE  R23 K111 R24 ; R23["token"] := R24
  462 [-]: GETUPVAL  R24 U6       ; R24 := U6
  463 [-]: MOVE      R25 R18      ; R25 := R18
  464 [-]: CALL      R24 2 2      ; R24 := R24(R25)
  465 [-]: SETTABLE  R23 K112 R24 ; R23["flock"] := R24
  466 [-]: GETUPVAL  R24 U9       ; R24 := U9
  467 [-]: CALL      R24 1 2      ; R24 := R24()
  468 [-]: SETTABLE  R23 K113 R24 ; R23["bdinfo"] := R24
  469 [-]: CALL      R21 3 1      ;  := R21(R22 to R23)
  470 [-]: RETURN    R0 1         ; return 
  471 [-]: GETUPVAL  R20 U0       ; R20 := U0
  472 [-]: GETTABLE  R20 R20 K100 ; R20 := R20["formvalue"]
  473 [-]: LOADK     R21 K114     ; R21 := "luci_language"
  474 [-]: CALL      R20 2 2      ; R20 := R20(R21)
  475 [-]: TEST      R20 0        ; if R20 then goto 477 else goto 491
  476 [-]: JMP       14           ; PC += 14 (goto 491)
  477 [-]: GETGLOBAL R21 K2       ; R21 := require
  478 [-]: LOADK     R22 K115     ; R22 := "luci.model.uci"
  479 [-]: CALL      R21 2 2      ; R21 := R21(R22)
  480 [-]: GETTABLE  R21 R21 K116 ; R21 := R21["cursor"]
  481 [-]: CALL      R21 1 2      ; R21 := R21()
  482 [-]: SELF      R22 R21 K117 ; R23 := R21; R22 := R21["set"]
  483 [-]: LOADK     R24 K47      ; R24 := "luci"
  484 [-]: LOADK     R25 K5       ; R25 := "main"
  485 [-]: LOADK     R26 K8       ; R26 := "lang"
  486 [-]: MOVE      R27 R20      ; R27 := R20
  487 [-]: CALL      R22 6 1      ;  := R22(R23 to R27)
  488 [-]: SELF      R22 R21 K118 ; R23 := R21; R22 := R21["commit"]
  489 [-]: LOADK     R24 K47      ; R24 := "luci"
  490 [-]: CALL      R22 3 1      ;  := R22(R23 to R24)
  491 [-]: GETUPVAL  R21 U0       ; R21 := U0
  492 [-]: GETTABLE  R21 R21 K100 ; R21 := R21["formvalue"]
  493 [-]: LOADK     R22 K119     ; R22 := "zonename"
  494 [-]: CALL      R21 2 2      ; R21 := R21(R22)
  495 [-]: TEST      R21 0        ; if R21 then goto 497 else goto 557
  496 [-]: JMP       60           ; PC += 60 (goto 557)
  497 [-]: GETGLOBAL R22 K2       ; R22 := require
  498 [-]: LOADK     R23 K115     ; R23 := "luci.model.uci"
  499 [-]: CALL      R22 2 2      ; R22 := R22(R23)
  500 [-]: GETTABLE  R22 R22 K116 ; R22 := R22["cursor"]
  501 [-]: CALL      R22 1 2      ; R22 := R22()
  502 [-]: SELF      R23 R22 K120 ; R24 := R22; R23 := R22["get"]
  503 [-]: LOADK     R25 K121     ; R25 := "system"
  504 [-]: LOADK     R26 K122     ; R26 := "@system[0]"
  505 [-]: LOADK     R27 K119     ; R27 := "zonename"
  506 [-]: CALL      R23 5 2      ; R23 := R23(R24 to R27)
  507 [-]: TEST      R23 1        ; if not R23 then goto 509 else goto 552
  508 [-]: JMP       43           ; PC += 43 (goto 552)
  509 [-]: GETGLOBAL R23 K2       ; R23 := require
  510 [-]: LOADK     R24 K123     ; R24 := "luci.sys.zoneinfo"
  511 [-]: CALL      R23 2 2      ; R23 := R23(R24)
  512 [-]: GETGLOBAL R24 K32      ; R24 := ipairs
  513 [-]: GETTABLE  R25 R23 K124 ; R25 := R23["TN"]
  514 [-]: CALL      R24 2 4      ; R24 to R26 := R24(R25)
  515 [-]: JMP       25           ; PC += 25 (goto 541)
  516 [-]: GETTABLE  R29 R28 K33  ; R29 := R28[Unknown_Type_Error]
  517 [-]: EQ        0 R29 R21    ; if R29 == R21 then goto 519 else goto 541
  518 [-]: JMP       22           ; PC += 22 (goto 541)
  519 [-]: SELF      R29 R22 K117 ; R30 := R22; R29 := R22["set"]
  520 [-]: LOADK     R31 K121     ; R31 := "system"
  521 [-]: LOADK     R32 K122     ; R32 := "@system[0]"
  522 [-]: LOADK     R33 K125     ; R33 := "timezone"
  523 [-]: GETTABLE  R34 R28 K126 ; R34 := R28[Unknown_Type_Error]
  524 [-]: CALL      R29 6 1      ;  := R29(R30 to R34)
  525 [-]: GETUPVAL  R29 U2       ; R29 := U2
  526 [-]: GETTABLE  R29 R29 K127 ; R29 := R29["writefile"]
  527 [-]: LOADK     R30 K128     ; R30 := "/etc/TZ"
  528 [-]: GETTABLE  R31 R28 K126 ; R31 := R28[Unknown_Type_Error]
  529 [-]: LOADK     R32 K129     ; R32 := "\n"
  530 [-]: CONCAT    R31 R31 R32  ; R31 := concat(R31 to R32)
  531 [-]: CALL      R29 3 1      ;  := R29(R30 to R31)
  532 [-]: GETUPVAL  R29 U3       ; R29 := U3
  533 [-]: GETTABLE  R29 R29 K130 ; R29 := R29["call"]
  534 [-]: LOADK     R30 K131     ; R30 := "date -k"
  535 [-]: CALL      R29 2 1      ;  := R29(R30)
  536 [-]: GETUPVAL  R29 U3       ; R29 := U3
  537 [-]: GETTABLE  R29 R29 K130 ; R29 := R29["call"]
  538 [-]: LOADK     R30 K132     ; R30 := "/etc/init.d/sysntpd restart"
  539 [-]: CALL      R29 2 1      ;  := R29(R30)
  540 [-]: JMP       2            ; PC += 2 (goto 543)
  541 [-]: TFORLOOP  R24 2        ; R27 to R28 := R24(R25,R26); if R27 ~= nil then R26 := R27 else goto 543
  542 [-]: JMP       -27          ; PC += -27 (goto 516)
  543 [-]: SELF      R24 R22 K117 ; R25 := R22; R24 := R22["set"]
  544 [-]: LOADK     R26 K121     ; R26 := "system"
  545 [-]: LOADK     R27 K122     ; R27 := "@system[0]"
  546 [-]: LOADK     R28 K119     ; R28 := "zonename"
  547 [-]: MOVE      R29 R21      ; R29 := R21
  548 [-]: CALL      R24 6 1      ;  := R24(R25 to R29)
  549 [-]: SELF      R24 R22 K118 ; R25 := R22; R24 := R22["commit"]
  550 [-]: LOADK     R26 K121     ; R26 := "system"
  551 [-]: CALL      R24 3 1      ;  := R24(R25 to R26)
  552 [-]: GETUPVAL  R23 U2       ; R23 := U2
  553 [-]: GETTABLE  R23 R23 K127 ; R23 := R23["writefile"]
  554 [-]: LOADK     R24 K133     ; R24 := "/tmp/zonename"
  555 [-]: MOVE      R25 R21      ; R25 := R21
  556 [-]: CALL      R23 3 1      ;  := R23(R24 to R25)
  557 [-]: GETUPVAL  R22 U2       ; R22 := U2
  558 [-]: GETTABLE  R22 R22 K134 ; R22 := R22["access"]
  559 [-]: LOADK     R23 K135     ; R23 := "/tmp/.timeclock"
  560 [-]: CALL      R22 2 2      ; R22 := R22(R23)
  561 [-]: TEST      R22 1        ; if not R22 then goto 563 else goto 594
  562 [-]: JMP       31           ; PC += 31 (goto 594)
  563 [-]: GETGLOBAL R22 K136     ; R22 := tonumber
  564 [-]: GETUPVAL  R23 U0       ; R23 := U0
  565 [-]: GETTABLE  R23 R23 K100 ; R23 := R23["formvalue"]
  566 [-]: LOADK     R24 K137     ; R24 := "timeclock"
  567 [-]: CALL      R23 2 0      ; R23 to top := R23(R24)
  568 [-]: CALL      R22 0 2      ; R22 := R22(R23 to top)
  569 [-]: TEST      R22 0        ; if R22 then goto 571 else goto 594
  570 [-]: JMP       23           ; PC += 23 (goto 594)
  571 [-]: GETGLOBAL R23 K138     ; R23 := os
  572 [-]: GETTABLE  R23 R23 K139 ; R23 := R23["date"]
  573 [-]: LOADK     R24 K140     ; R24 := "*t"
  574 [-]: MOVE      R25 R22      ; R25 := R22
  575 [-]: CALL      R23 3 2      ; R23 := R23(R24 to R25)
  576 [-]: TEST      R23 0        ; if R23 then goto 578 else goto 594
  577 [-]: JMP       16           ; PC += 16 (goto 594)
  578 [-]: GETUPVAL  R24 U3       ; R24 := U3
  579 [-]: GETTABLE  R24 R24 K130 ; R24 := R24["call"]
  580 [-]: NEWTABLE  R25 6 0      ; R25 := {} (size = 6,0)
  581 [-]: GETTABLE  R26 R23 K142 ; R26 := R23["year"]
  582 [-]: GETTABLE  R27 R23 K143 ; R27 := R23["month"]
  583 [-]: GETTABLE  R28 R23 K144 ; R28 := R23["day"]
  584 [-]: GETTABLE  R29 R23 K145 ; R29 := R23["hour"]
  585 [-]: GETTABLE  R30 R23 K146 ; R30 := R23["min"]
  586 [-]: GETTABLE  R31 R23 K147 ; R31 := R23["sec"]
  587 [-]: SETLIST   R25 6 1      ; R25[0] to R25[5] := R26 to R31 ; R(a)[(c-1)*FPF+i] := R(a+i), 1 <= i <= b, a=25, b=6, c=1, FPF=50
  588 [-]: MOD       R25 K141 R25 ; R25 := "date -s \'%04d-%02d-%02d %02d:%02d:%02d\'" % R25
  589 [-]: CALL      R24 2 1      ;  := R24(R25)
  590 [-]: GETUPVAL  R24 U3       ; R24 := U3
  591 [-]: GETTABLE  R24 R24 K148 ; R24 := R24["fork_exec"]
  592 [-]: LOADK     R25 K149     ; R25 := "env -i ACTION=\'setclock\' /sbin/hotplug-call ntp"
  593 [-]: CALL      R24 2 1      ;  := R24(R25)
  594 [-]: GETUPVAL  R22 U0       ; R22 := U0
  595 [-]: GETTABLE  R22 R22 K150 ; R22 := R22["header"]
  596 [-]: LOADK     R23 K151     ; R23 := "Set-Cookie"
  597 [-]: NEWTABLE  R24 1 0      ; R24 := {} (size = 1,0)
  598 [-]: MOVE      R25 R14      ; R25 := R14
  599 [-]: GETGLOBAL R26 K74      ; R26 := build_url
  600 [-]: CALL      R26 1 0      ; R26 to top := R26()
  601 [-]: SETLIST   R24 0 1      ; R24[0] to R24[top] := R25 to top ; R(a)[(c-1)*FPF+i] := R(a+i), 1 <= i <= b, a=24, b=0, c=1, FPF=50
  602 [-]: MOD       R24 K152 R24 ; R24 := "sysauth=%s; path=%s; HttpOnly" % R24
  603 [-]: CALL      R22 3 1      ;  := R22(R23 to R24)
  604 [-]: GETUPVAL  R22 U0       ; R22 := U0
  605 [-]: GETTABLE  R22 R22 K153 ; R22 := R22["redirect"]
  606 [-]: GETGLOBAL R23 K74      ; R23 := build_url
  607 [-]: GETGLOBAL R24 K154     ; R24 := unpack
  608 [-]: GETTABLE  R25 R1 K39   ; R25 := R1["requestpath"]
  609 [-]: CALL      R24 2 0      ; R24 to top := R24(R25)
  610 [-]: CALL      R23 0 0      ; R23 to top := R23(R24 to top)
  611 [-]: CALL      R22 0 1      ;  := R22(R23 to top)
  612 [-]: TEST      R14 0        ; if R14 then goto 614 else goto 616
  613 [-]: JMP       2            ; PC += 2 (goto 616)
  614 [-]: TEST      R15 1        ; if not R15 then goto 616 else goto 622
  615 [-]: JMP       6            ; PC += 6 (goto 622)
  616 [-]: GETUPVAL  R18 U0       ; R18 := U0
  617 [-]: GETTABLE  R18 R18 K104 ; R18 := R18["status"]
  618 [-]: LOADK     R19 K105     ; R19 := Unknown_Type_Error
  619 [-]: LOADK     R20 K106     ; R20 := "Forbidden"
  620 [-]: CALL      R18 3 1      ;  := R18(R19 to R20)
  621 [-]: RETURN    R0 1         ; return 
  622 [-]: GETUPVAL  R18 U10      ; R18 := U10
  623 [-]: GETTABLE  R18 R18 K1   ; R18 := R18["path"]
  624 [-]: GETTABLE  R19 R15 K155 ; R19 := R15["username"]
  625 [-]: GETGLOBAL R20 K79      ; R20 := table
  626 [-]: GETTABLE  R20 R20 K80  ; R20 := R20["concat"]
  627 [-]: MOVE      R21 R0       ; R21 := R0
  628 [-]: LOADK     R22 K81      ; R22 := "/"
  629 [-]: CALL      R20 3 0      ; R20 to top := R20(R21 to R22)
  630 [-]: CALL      R18 0 2      ; R18 := R18(R19 to top)
  631 [-]: TEST      R18 0        ; if R18 then goto 633 else goto 639
  632 [-]: JMP       6            ; PC += 6 (goto 639)
  633 [-]: GETUPVAL  R18 U0       ; R18 := U0
  634 [-]: GETTABLE  R18 R18 K104 ; R18 := R18["status"]
  635 [-]: LOADK     R19 K105     ; R19 := Unknown_Type_Error
  636 [-]: LOADK     R20 K106     ; R20 := "Forbidden"
  637 [-]: CALL      R18 3 1      ;  := R18(R19 to R20)
  638 [-]: RETURN    R0 1         ; return 
  639 [-]: SETTABLE  R1 K156 R14  ; R1["authsession"] := R14
  640 [-]: GETTABLE  R18 R15 K111 ; R18 := R15["token"]
  641 [-]: SETTABLE  R1 K157 R18  ; R1["authtoken"] := R18
  642 [-]: GETTABLE  R18 R15 K155 ; R18 := R15["username"]
  643 [-]: SETTABLE  R1 K158 R18  ; R1["authuser"] := R18
  644 [-]: TEST      R5 0         ; if R5 then goto 646 else goto 657
  645 [-]: JMP       11           ; PC += 11 (goto 657)
  646 [-]: GETUPVAL  R12 U11      ; R12 := U11
  647 [-]: GETTABLE  R13 R5 K159  ; R13 := R5["target"]
  648 [-]: CALL      R12 2 2      ; R12 := R12(R13)
  649 [-]: TEST      R12 0        ; if R12 then goto 651 else goto 657
  650 [-]: JMP       6            ; PC += 6 (goto 657)
  651 [-]: GETGLOBAL R12 K160     ; R12 := test_post_security
  652 [-]: MOVE      R13 R5       ; R13 := R5
  653 [-]: CALL      R12 2 2      ; R12 := R12(R13)
  654 [-]: TEST      R12 1        ; if not R12 then goto 656 else goto 657
  655 [-]: JMP       1            ; PC += 1 (goto 657)
  656 [-]: RETURN    R0 1         ; return 
  657 [-]: GETTABLE  R12 R7 K161  ; R12 := R7["setgroup"]
  658 [-]: TEST      R12 0        ; if R12 then goto 660 else goto 665
  659 [-]: JMP       5            ; PC += 5 (goto 665)
  660 [-]: GETUPVAL  R12 U3       ; R12 := U3
  661 [-]: GETTABLE  R12 R12 K162 ; R12 := R12["process"]
  662 [-]: GETTABLE  R12 R12 K161 ; R12 := R12["setgroup"]
  663 [-]: GETTABLE  R13 R7 K161  ; R13 := R7["setgroup"]
  664 [-]: CALL      R12 2 1      ;  := R12(R13)
  665 [-]: GETTABLE  R12 R7 K163  ; R12 := R7["setuser"]
  666 [-]: TEST      R12 0        ; if R12 then goto 668 else goto 673
  667 [-]: JMP       5            ; PC += 5 (goto 673)
  668 [-]: GETUPVAL  R12 U3       ; R12 := U3
  669 [-]: GETTABLE  R12 R12 K162 ; R12 := R12["process"]
  670 [-]: GETTABLE  R12 R12 K163 ; R12 := R12["setuser"]
  671 [-]: GETTABLE  R13 R7 K163  ; R13 := R7["setuser"]
  672 [-]: CALL      R12 2 1      ;  := R12(R13)
  673 [-]: LOADNIL   R12 R12      ; R12 := nil
  674 [-]: TEST      R5 0         ; if R5 then goto 676 else goto 690
  675 [-]: JMP       14           ; PC += 14 (goto 690)
  676 [-]: GETGLOBAL R13 K88      ; R13 := type
  677 [-]: GETTABLE  R14 R5 K159  ; R14 := R5["target"]
  678 [-]: CALL      R13 2 2      ; R13 := R13(R14)
  679 [-]: EQ        0 R13 K93    ; if R13 == "function" then goto 681 else goto 683
  680 [-]: JMP       2            ; PC += 2 (goto 683)
  681 [-]: GETTABLE  R12 R5 K159  ; R12 := R5["target"]
  682 [-]: JMP       7            ; PC += 7 (goto 690)
  683 [-]: GETGLOBAL R13 K88      ; R13 := type
  684 [-]: GETTABLE  R14 R5 K159  ; R14 := R5["target"]
  685 [-]: CALL      R13 2 2      ; R13 := R13(R14)
  686 [-]: EQ        0 R13 K79    ; if R13 == "table" then goto 688 else goto 690
  687 [-]: JMP       2            ; PC += 2 (goto 690)
  688 [-]: GETTABLE  R13 R5 K159  ; R13 := R5["target"]
  689 [-]: GETTABLE  R12 R13 K159 ; R12 := R13["target"]
  690 [-]: TEST      R5 0         ; if R5 then goto 692 else goto 706
  691 [-]: JMP       14           ; PC += 14 (goto 706)
  692 [-]: GETTABLE  R13 R5 K43   ; R13 := R5["index"]
  693 [-]: TEST      R13 1        ; if not R13 then goto 695 else goto 700
  694 [-]: JMP       5            ; PC += 5 (goto 700)
  695 [-]: GETGLOBAL R13 K88      ; R13 := type
  696 [-]: MOVE      R14 R12      ; R14 := R12
  697 [-]: CALL      R13 2 2      ; R13 := R13(R14)
  698 [-]: EQ        0 R13 K93    ; if R13 == "function" then goto 700 else goto 706
  699 [-]: JMP       6            ; PC += 6 (goto 706)
  700 [-]: SETTABLE  R1 K164 R5   ; R1["dispatched"] := R5
  701 [-]: GETTABLE  R13 R1 K165  ; R13 := R1["requested"]
  702 [-]: TEST      R13 1        ; if not R13 then goto 704 else goto 705
  703 [-]: JMP       1            ; PC += 1 (goto 705)
  704 [-]: GETTABLE  R13 R1 K164  ; R13 := R1["dispatched"]
  705 [-]: SETTABLE  R1 K165 R13  ; R1["requested"] := R13
  706 [-]: TEST      R5 0         ; if R5 then goto 708 else goto 724
  707 [-]: JMP       16           ; PC += 16 (goto 724)
  708 [-]: GETTABLE  R13 R5 K43   ; R13 := R5["index"]
  709 [-]: TEST      R13 0        ; if R13 then goto 711 else goto 724
  710 [-]: JMP       13           ; PC += 13 (goto 724)
  711 [-]: GETGLOBAL R13 K2       ; R13 := require
  712 [-]: LOADK     R14 K45      ; R14 := "luci.template"
  713 [-]: CALL      R13 2 2      ; R13 := R13(R14)
  714 [-]: GETUPVAL  R14 U1       ; R14 := U1
  715 [-]: GETTABLE  R14 R14 K166 ; R14 := R14["copcall"]
  716 [-]: GETTABLE  R15 R13 K107 ; R15 := R13["render"]
  717 [-]: LOADK     R16 K167     ; R16 := "indexer"
  718 [-]: NEWTABLE  R17 0 0      ; R17 := {} (size = 0,0)
  719 [-]: CALL      R14 4 2      ; R14 := R14(R15 to R17)
  720 [-]: TEST      R14 0        ; if R14 then goto 722 else goto 724
  721 [-]: JMP       2            ; PC += 2 (goto 724)
  722 [-]: LOADBOOL  R14 1 0      ; R14 := true
  723 [-]: RETURN    R14 2        ; return R14
  724 [-]: GETGLOBAL R13 K88      ; R13 := type
  725 [-]: MOVE      R14 R12      ; R14 := R12
  726 [-]: CALL      R13 2 2      ; R13 := R13(R14)
  727 [-]: EQ        0 R13 K93    ; if R13 == "function" then goto 729 else goto 793
  728 [-]: JMP       64           ; PC += 64 (goto 793)
  729 [-]: GETUPVAL  R13 U1       ; R13 := U1
  730 [-]: GETTABLE  R13 R13 K166 ; R13 := R13["copcall"]
  731 [-]: CLOSURE   R14 6        ; R14 := closure(Function #0_19_6)
  732 [-]: MOVE      R0 R12       ; R0 := R12
  733 [-]: MOVE      R0 R5        ; R0 := R5
  734 [-]: CALL      R13 2 1      ;  := R13(R14)
  735 [-]: LOADNIL   R13 R14      ; R13 to R14 := nil
  736 [-]: GETGLOBAL R15 K88      ; R15 := type
  737 [-]: GETTABLE  R16 R5 K159  ; R16 := R5["target"]
  738 [-]: CALL      R15 2 2      ; R15 := R15(R16)
  739 [-]: EQ        0 R15 K79    ; if R15 == "table" then goto 741 else goto 752
  740 [-]: JMP       11           ; PC += 11 (goto 752)
  741 [-]: GETUPVAL  R15 U1       ; R15 := U1
  742 [-]: GETTABLE  R15 R15 K166 ; R15 := R15["copcall"]
  743 [-]: MOVE      R16 R12      ; R16 := R12
  744 [-]: GETTABLE  R17 R5 K159  ; R17 := R5["target"]
  745 [-]: GETGLOBAL R18 K154     ; R18 := unpack
  746 [-]: MOVE      R19 R8       ; R19 := R8
  747 [-]: CALL      R18 2 0      ; R18 to top := R18(R19)
  748 [-]: CALL      R15 0 3      ; R15 to R16 := R15(R16 to top)
  749 [-]: MOVE      R14 R16      ; R14 := R16
  750 [-]: MOVE      R13 R15      ; R13 := R15
  751 [-]: JMP       9            ; PC += 9 (goto 761)
  752 [-]: GETUPVAL  R15 U1       ; R15 := U1
  753 [-]: GETTABLE  R15 R15 K166 ; R15 := R15["copcall"]
  754 [-]: MOVE      R16 R12      ; R16 := R12
  755 [-]: GETGLOBAL R17 K154     ; R17 := unpack
  756 [-]: MOVE      R18 R8       ; R18 := R8
  757 [-]: CALL      R17 2 0      ; R17 to top := R17(R18)
  758 [-]: CALL      R15 0 3      ; R15 to R16 := R15(R16 to top)
  759 [-]: MOVE      R14 R16      ; R14 := R16
  760 [-]: MOVE      R13 R15      ; R13 := R15
  761 [-]: GETGLOBAL R15 K4       ; R15 := assert
  762 [-]: MOVE      R16 R13      ; R16 := R13
  763 [-]: LOADK     R17 K168     ; R17 := "Failed to execute "
  764 [-]: GETGLOBAL R18 K88      ; R18 := type
  765 [-]: GETTABLE  R19 R5 K159  ; R19 := R5["target"]
  766 [-]: CALL      R18 2 2      ; R18 := R18(R19)
  767 [-]: EQ        0 R18 K93    ; if R18 == "function" then goto 769 else goto 772
  768 [-]: JMP       3            ; PC += 3 (goto 772)
  769 [-]: LOADK     R18 K93      ; R18 := "function"
  770 [-]: TEST      R18 1        ; if not R18 then goto 772 else goto 777
  771 [-]: JMP       5            ; PC += 5 (goto 777)
  772 [-]: GETTABLE  R18 R5 K159  ; R18 := R5["target"]
  773 [-]: GETTABLE  R18 R18 K88  ; R18 := R18["type"]
  774 [-]: TEST      R18 1        ; if not R18 then goto 776 else goto 777
  775 [-]: JMP       1            ; PC += 1 (goto 777)
  776 [-]: LOADK     R18 K169     ; R18 := "unknown"
  777 [-]: LOADK     R19 K170     ; R19 := " dispatcher target for entry \'/"
  778 [-]: GETGLOBAL R20 K79      ; R20 := table
  779 [-]: GETTABLE  R20 R20 K80  ; R20 := R20["concat"]
  780 [-]: MOVE      R21 R0       ; R21 := R0
  781 [-]: LOADK     R22 K81      ; R22 := "/"
  782 [-]: CALL      R20 3 2      ; R20 := R20(R21 to R22)
  783 [-]: LOADK     R21 K171     ; R21 := "\'.\n"
  784 [-]: LOADK     R22 K172     ; R22 := "The called action terminated with an exception:\n"
  785 [-]: GETGLOBAL R23 K173     ; R23 := tostring
  786 [-]: TESTSET   R24 R14 1    ; if R14 then R24 := R14 ; goto 789 else goto 788
  787 [-]: JMP       1            ; PC += 1 (goto 789)
  788 [-]: LOADK     R24 K174     ; R24 := "(unknown)"
  789 [-]: CALL      R23 2 2      ; R23 := R23(R24)
  790 [-]: CONCAT    R17 R17 R23  ; R17 := concat(R17 to R23)
  791 [-]: CALL      R15 3 1      ;  := R15(R16 to R17)
  792 [-]: JMP       26           ; PC += 26 (goto 819)
  793 [-]: GETGLOBAL R13 K175     ; R13 := node
  794 [-]: CALL      R13 1 2      ; R13 := R13()
  795 [-]: TEST      R13 0        ; if R13 then goto 797 else goto 800
  796 [-]: JMP       3            ; PC += 3 (goto 800)
  797 [-]: GETTABLE  R14 R13 K159 ; R14 := R13["target"]
  798 [-]: TEST      R14 1        ; if not R14 then goto 800 else goto 807
  799 [-]: JMP       7            ; PC += 7 (goto 807)
  800 [-]: GETGLOBAL R14 K176     ; R14 := error404
  801 [-]: LOADK     R15 K177     ; R15 := "No root node was registered, this usually happens if no module was installed.\n"
  802 [-]: LOADK     R16 K178     ; R16 := "Install luci-mod-admin-full and retry. "
  803 [-]: LOADK     R17 K179     ; R17 := "If the module is already installed, try removing the /tmp/luci-indexcache file."
  804 [-]: CONCAT    R15 R15 R17  ; R15 := concat(R15 to R17)
  805 [-]: CALL      R14 2 1      ;  := R14(R15)
  806 [-]: JMP       12           ; PC += 12 (goto 819)
  807 [-]: GETGLOBAL R14 K176     ; R14 := error404
  808 [-]: LOADK     R15 K180     ; R15 := "No page is registered at \'/"
  809 [-]: GETGLOBAL R16 K79      ; R16 := table
  810 [-]: GETTABLE  R16 R16 K80  ; R16 := R16["concat"]
  811 [-]: MOVE      R17 R0       ; R17 := R0
  812 [-]: LOADK     R18 K81      ; R18 := "/"
  813 [-]: CALL      R16 3 2      ; R16 := R16(R17 to R18)
  814 [-]: LOADK     R17 K171     ; R17 := "\'.\n"
  815 [-]: LOADK     R18 K181     ; R18 := "If this url belongs to an extension, make sure it is properly installed.\n"
  816 [-]: LOADK     R19 K182     ; R19 := "If the extension was recently installed, try removing the /tmp/luci-indexcache file."
  817 [-]: CONCAT    R15 R15 R19  ; R15 := concat(R15 to R19)
  818 [-]: CALL      R14 2 1      ;  := R14(R15)
  819 [-]: RETURN    R0 1         ; return 


; Function:        0_19_0
; Defined at line: 537
; #Upvalues:       1
; #Parameters:     3
; Is_vararg:       0
; Max Stack Size:  12

    0 [-]: TEST      R0 0         ; if R0 then goto 2 else goto 67
    1 [-]: JMP       65           ; PC += 65 (goto 67)
    2 [-]: GETGLOBAL R3 K0        ; R3 := getfenv
    3 [-]: LOADK     R4 K1        ; R4 := Unknown_Type_Error
    4 [-]: CALL      R3 2 2       ; R3 := R3(R4)
    5 [-]: GETGLOBAL R4 K2        ; R4 := type
    6 [-]: GETTABLE  R5 R3 K3     ; R5 := R3["self"]
    7 [-]: CALL      R4 2 2       ; R4 := R4(R5)
    8 [-]: EQ        0 R4 K4      ; if R4 == "table" then goto 10 else goto 12
    9 [-]: JMP       2            ; PC += 2 (goto 12)
   10 [-]: GETTABLE  R4 R3 K3     ; R4 := R3["self"]
   11 [-]: JMP       2            ; PC += 2 (goto 14)
   12 [-]: LOADBOOL  R4 0 1       ; R4 := false; goto 14
   13 [-]: LOADBOOL  R4 1 0       ; R4 := true
   14 [-]: GETGLOBAL R5 K2        ; R5 := type
   15 [-]: MOVE      R6 R2        ; R6 := R2
   16 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   17 [-]: EQ        0 R5 K4      ; if R5 == "table" then goto 19 else goto 32
   18 [-]: JMP       13           ; PC += 13 (goto 32)
   19 [-]: GETGLOBAL R5 K5        ; R5 := next
   20 [-]: MOVE      R6 R2        ; R6 := R2
   21 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   22 [-]: TEST      R5 1         ; if not R5 then goto 24 else goto 27
   23 [-]: JMP       3            ; PC += 3 (goto 27)
   24 [-]: LOADK     R5 K6        ; R5 := ""
   25 [-]: RETURN    R5 2         ; return R5
   26 [-]: JMP       5            ; PC += 5 (goto 32)
   27 [-]: GETUPVAL  R5 U0        ; R5 := U0
   28 [-]: GETTABLE  R5 R5 K7     ; R5 := R5["serialize_json"]
   29 [-]: MOVE      R6 R2        ; R6 := R2
   30 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   31 [-]: MOVE      R2 R5        ; R2 := R5
   32 [-]: GETGLOBAL R5 K8        ; R5 := string
   33 [-]: GETTABLE  R5 R5 K9     ; R5 := R5["format"]
   34 [-]: LOADK     R6 K10       ; R6 := " %s=\"%s\""
   35 [-]: GETGLOBAL R7 K11       ; R7 := tostring
   36 [-]: MOVE      R8 R1        ; R8 := R1
   37 [-]: CALL      R7 2 2       ; R7 := R7(R8)
   38 [-]: GETUPVAL  R8 U0        ; R8 := U0
   39 [-]: GETTABLE  R8 R8 K12    ; R8 := R8["pcdata"]
   40 [-]: GETGLOBAL R9 K11       ; R9 := tostring
   41 [-]: TESTSET   R10 R2 1     ; if R2 then R10 := R2 ; goto 62 else goto 43
   42 [-]: JMP       19           ; PC += 19 (goto 62)
   43 [-]: GETGLOBAL R10 K2       ; R10 := type
   44 [-]: GETTABLE  R11 R3 R1    ; R11 := R3[R1]
   45 [-]: CALL      R10 2 2      ; R10 := R10(R11)
   46 [-]: EQ        1 R10 K13    ; if R10 ~= "function" then goto 48 else goto 51
   47 [-]: JMP       3            ; PC += 3 (goto 51)
   48 [-]: GETTABLE  R10 R3 R1    ; R10 := R3[R1]
   49 [-]: TEST      R10 1        ; if not R10 then goto 51 else goto 62
   50 [-]: JMP       11           ; PC += 11 (goto 62)
   51 [-]: TEST      R4 0         ; if R4 then goto 53 else goto 61
   52 [-]: JMP       8            ; PC += 8 (goto 61)
   53 [-]: GETGLOBAL R10 K2       ; R10 := type
   54 [-]: GETTABLE  R11 R4 R1    ; R11 := R4[R1]
   55 [-]: CALL      R10 2 2      ; R10 := R10(R11)
   56 [-]: EQ        1 R10 K13    ; if R10 ~= "function" then goto 58 else goto 61
   57 [-]: JMP       3            ; PC += 3 (goto 61)
   58 [-]: GETTABLE  R10 R4 R1    ; R10 := R4[R1]
   59 [-]: TEST      R10 1        ; if not R10 then goto 61 else goto 62
   60 [-]: JMP       1            ; PC += 1 (goto 62)
   61 [-]: LOADK     R10 K6       ; R10 := ""
   62 [-]: CALL      R9 2 0       ; R9 to top := R9(R10)
   63 [-]: CALL      R8 0 0       ; R8 to top := R8(R9 to top)
   64 [-]: TAILCALL  R5 0 0       ; R5 to top := R5(R6 to top)
   65 [-]: RETURN    R5 0         ; return R5 to top
   66 [-]: JMP       2            ; PC += 2 (goto 69)
   67 [-]: LOADK     R3 K6        ; R3 := ""
   68 [-]: RETURN    R3 2         ; return R3
   69 [-]: RETURN    R0 1         ; return 


; Function:        0_19_1
; Defined at line: 562
; #Upvalues:       1
; #Parameters:     1
; Is_vararg:       0
; Max Stack Size:  5

    0 [-]: GETUPVAL  R1 U0        ; R1 := U0
    1 [-]: GETTABLE  R1 R1 K0     ; R1 := R1["Template"]
    2 [-]: MOVE      R2 R0        ; R2 := R0
    3 [-]: CALL      R1 2 2       ; R1 := R1(R2)
    4 [-]: SELF      R1 R1 K1     ; R2 := R1; R1 := R1["render"]
    5 [-]: GETGLOBAL R3 K2        ; R3 := getfenv
    6 [-]: LOADK     R4 K3        ; R4 := Unknown_Type_Error
    7 [-]: CALL      R3 2 0       ; R3 to top := R3(R4)
    8 [-]: CALL      R1 0 1       ;  := R1(R2 to top)
    9 [-]: RETURN    R0 1         ; return 


; Function:        0_19_2
; Defined at line: 565
; #Upvalues:       1
; #Parameters:     2
; Is_vararg:       0
; Max Stack Size:  3

    0 [-]: GETUPVAL  R2 U0        ; R2 := U0
    1 [-]: GETTABLE  R2 R2 K0     ; R2 := R2["context"]
    2 [-]: GETTABLE  R2 R2 K1     ; R2 := R2["viewns"]
    3 [-]: GETTABLE  R2 R2 R0     ; R2 := R2[R0]
    4 [-]: EQ        0 R2 K2      ; if R2 == nil then goto 6 else goto 10
    5 [-]: JMP       4            ; PC += 4 (goto 10)
    6 [-]: GETUPVAL  R2 U0        ; R2 := U0
    7 [-]: GETTABLE  R2 R2 K0     ; R2 := R2["context"]
    8 [-]: GETTABLE  R2 R2 K1     ; R2 := R2["viewns"]
    9 [-]: SETTABLE  R2 R0 R1     ; R2[R0] := R1
   10 [-]: RETURN    R0 1         ; return 


; Function:        0_19_3
; Defined at line: 571
; #Upvalues:       1
; #Parameters:     0
; Is_vararg:       3
; Max Stack Size:  3

    0 [-]: GETUPVAL  R1 U0        ; R1 := U0
    1 [-]: VARARG    R2 0         ; R2 to top := ...
    2 [-]: TAILCALL  R1 0 0       ; R1 to top := R1(R2 to top)
    3 [-]: RETURN    R1 0         ; return R1 to top
    4 [-]: RETURN    R0 1         ; return 


; Function:        0_19_4
; Defined at line: 572
; #Upvalues:       1
; #Parameters:     0
; Is_vararg:       3
; Max Stack Size:  4

    0 [-]: GETUPVAL  R1 U0        ; R1 := U0
    1 [-]: LOADBOOL  R2 1 0       ; R2 := true
    2 [-]: VARARG    R3 0         ; R3 to top := ...
    3 [-]: TAILCALL  R1 0 0       ; R1 to top := R1(R2 to top)
    4 [-]: RETURN    R1 0         ; return R1 to top
    5 [-]: RETURN    R0 1         ; return 


; Function:        0_19_5
; Defined at line: 574
; #Upvalues:       1
; #Parameters:     2
; Is_vararg:       0
; Max Stack Size:  5

    0 [-]: EQ        0 R1 K0      ; if R1 == "controller" then goto 2 else goto 6
    1 [-]: JMP       4            ; PC += 4 (goto 6)
    2 [-]: GETGLOBAL R2 K1        ; R2 := build_url
    3 [-]: TAILCALL  R2 1 0       ; R2 to top := R2()
    4 [-]: RETURN    R2 0         ; return R2 to top
    5 [-]: JMP       25           ; PC += 25 (goto 31)
    6 [-]: EQ        0 R1 K2      ; if R1 == "REQUEST_URI" then goto 8 else goto 16
    7 [-]: JMP       8            ; PC += 8 (goto 16)
    8 [-]: GETGLOBAL R2 K1        ; R2 := build_url
    9 [-]: GETGLOBAL R3 K3        ; R3 := unpack
   10 [-]: GETUPVAL  R4 U0        ; R4 := U0
   11 [-]: GETTABLE  R4 R4 K4     ; R4 := R4["requestpath"]
   12 [-]: CALL      R3 2 0       ; R3 to top := R3(R4)
   13 [-]: TAILCALL  R2 0 0       ; R2 to top := R2(R3 to top)
   14 [-]: RETURN    R2 0         ; return R2 to top
   15 [-]: JMP       15           ; PC += 15 (goto 31)
   16 [-]: EQ        0 R1 K5      ; if R1 == "token" then goto 18 else goto 22
   17 [-]: JMP       4            ; PC += 4 (goto 22)
   18 [-]: GETUPVAL  R2 U0        ; R2 := U0
   19 [-]: GETTABLE  R2 R2 K6     ; R2 := R2["authtoken"]
   20 [-]: RETURN    R2 2         ; return R2
   21 [-]: JMP       9            ; PC += 9 (goto 31)
   22 [-]: GETGLOBAL R2 K7        ; R2 := rawget
   23 [-]: MOVE      R3 R0        ; R3 := R0
   24 [-]: MOVE      R4 R1        ; R4 := R1
   25 [-]: CALL      R2 3 2       ; R2 := R2(R3 to R4)
   26 [-]: TEST      R2 1         ; if not R2 then goto 28 else goto 30
   27 [-]: JMP       2            ; PC += 2 (goto 30)
   28 [-]: GETGLOBAL R2 K8        ; R2 := _G
   29 [-]: GETTABLE  R2 R2 R1     ; R2 := R2[R1]
   30 [-]: RETURN    R2 2         ; return R2
   31 [-]: RETURN    R0 1         ; return 


; Function:        0_19_6
; Defined at line: 752
; #Upvalues:       2
; #Parameters:     0
; Is_vararg:       0
; Max Stack Size:  6

    0 [-]: GETGLOBAL R0 K0        ; R0 := getfenv
    1 [-]: GETUPVAL  R1 U0        ; R1 := U0
    2 [-]: CALL      R0 2 2       ; R0 := R0(R1)
    3 [-]: GETGLOBAL R1 K1        ; R1 := require
    4 [-]: GETUPVAL  R2 U1        ; R2 := U1
    5 [-]: GETTABLE  R2 R2 K2     ; R2 := R2["module"]
    6 [-]: CALL      R1 2 2       ; R1 := R1(R2)
    7 [-]: GETGLOBAL R2 K3        ; R2 := setmetatable
    8 [-]: NEWTABLE  R3 0 0       ; R3 := {} (size = 0,0)
    9 [-]: NEWTABLE  R4 0 1       ; R4 := {} (size = 0,1)
   10 [-]: CLOSURE   R5 0         ; R5 := closure(Function #0_19_6_0)
   11 [-]: MOVE      R0 R1        ; R0 := R1
   12 [-]: MOVE      R0 R0        ; R0 := R0
   13 [-]: SETTABLE  R4 K4 R5     ; R4["__index"] := R5
   14 [-]: CALL      R2 3 2       ; R2 := R2(R3 to R4)
   15 [-]: GETGLOBAL R3 K5        ; R3 := setfenv
   16 [-]: GETUPVAL  R4 U0        ; R4 := U0
   17 [-]: MOVE      R5 R2        ; R5 := R2
   18 [-]: CALL      R3 3 1       ;  := R3(R4 to R5)
   19 [-]: RETURN    R0 1         ; return 


; Function:        0_19_6_0
; Defined at line: 757
; #Upvalues:       2
; #Parameters:     2
; Is_vararg:       0
; Max Stack Size:  5

    0 [-]: GETGLOBAL R2 K0        ; R2 := rawget
    1 [-]: MOVE      R3 R0        ; R3 := R0
    2 [-]: MOVE      R4 R1        ; R4 := R1
    3 [-]: CALL      R2 3 2       ; R2 := R2(R3 to R4)
    4 [-]: TEST      R2 1         ; if not R2 then goto 6 else goto 12
    5 [-]: JMP       6            ; PC += 6 (goto 12)
    6 [-]: GETUPVAL  R2 U0        ; R2 := U0
    7 [-]: GETTABLE  R2 R2 R1     ; R2 := R2[R1]
    8 [-]: TEST      R2 1         ; if not R2 then goto 10 else goto 12
    9 [-]: JMP       2            ; PC += 2 (goto 12)
   10 [-]: GETUPVAL  R2 U1        ; R2 := U1
   11 [-]: GETTABLE  R2 R2 R1     ; R2 := R2[R1]
   12 [-]: RETURN    R2 2         ; return R2
   13 [-]: RETURN    R0 1         ; return 


; Function:        0_20
; Defined at line: 788
; #Upvalues:       5
; #Parameters:     0
; Is_vararg:       0
; Max Stack Size:  20

    0 [-]: NEWTABLE  R0 0 0       ; R0 := {} (size = 0,0)
    1 [-]: GETUPVAL  R1 U0        ; R1 := U0
    2 [-]: GETTABLE  R1 R1 K1     ; R1 := R1["libpath"]
    3 [-]: CALL      R1 1 2       ; R1 := R1()
    4 [-]: MOD       R1 K0 R1     ; R1 := "%s/controller/" % R1
    5 [-]: LOADNIL   R2 R3        ; R2 to R3 := nil
    6 [-]: GETUPVAL  R4 U1        ; R4 := U1
    7 [-]: GETTABLE  R4 R4 K2     ; R4 := R4["glob"]
    8 [-]: MOD       R5 K3 R1     ; R5 := "%s*.lua" % R1
    9 [-]: CALL      R4 2 2       ; R4 := R4(R5)
   10 [-]: TEST      R4 1         ; if not R4 then goto 12 else goto 13
   11 [-]: JMP       1            ; PC += 1 (goto 13)
   12 [-]: CLOSURE   R4 0         ; R4 := closure(Function #0_20_0)
   13 [-]: LOADNIL   R5 R6        ; R5 to R6 := nil
   14 [-]: JMP       3            ; PC += 3 (goto 18)
   15 [-]: LEN       R8 R0        ; R8 := #R0
   16 [-]: ADD       R8 R8 K4     ; R8 := R8 + Unknown_Type_Error
   17 [-]: SETTABLE  R0 R8 R7     ; R0[R8] := R7
   18 [-]: TFORLOOP  R4 1         ; R7 := R4(R5,R6); if R7 ~= nil then R6 := R7 else goto 20
   19 [-]: JMP       -5           ; PC += -5 (goto 15)
   20 [-]: GETUPVAL  R4 U1        ; R4 := U1
   21 [-]: GETTABLE  R4 R4 K2     ; R4 := R4["glob"]
   22 [-]: MOD       R5 K5 R1     ; R5 := "%s*/*.lua" % R1
   23 [-]: CALL      R4 2 2       ; R4 := R4(R5)
   24 [-]: TEST      R4 1         ; if not R4 then goto 26 else goto 27
   25 [-]: JMP       1            ; PC += 1 (goto 27)
   26 [-]: CLOSURE   R4 1         ; R4 := closure(Function #0_20_1)
   27 [-]: LOADNIL   R5 R6        ; R5 to R6 := nil
   28 [-]: JMP       3            ; PC += 3 (goto 32)
   29 [-]: LEN       R8 R0        ; R8 := #R0
   30 [-]: ADD       R8 R8 K4     ; R8 := R8 + Unknown_Type_Error
   31 [-]: SETTABLE  R0 R8 R7     ; R0[R8] := R7
   32 [-]: TFORLOOP  R4 1         ; R7 := R4(R5,R6); if R7 ~= nil then R6 := R7 else goto 34
   33 [-]: JMP       -5           ; PC += -5 (goto 29)
   34 [-]: GETGLOBAL R4 K6        ; R4 := indexcache
   35 [-]: TEST      R4 0         ; if R4 then goto 37 else goto 105
   36 [-]: JMP       68           ; PC += 68 (goto 105)
   37 [-]: GETUPVAL  R4 U1        ; R4 := U1
   38 [-]: GETTABLE  R4 R4 K7     ; R4 := R4["stat"]
   39 [-]: GETGLOBAL R5 K6        ; R5 := indexcache
   40 [-]: LOADK     R6 K8        ; R6 := "mtime"
   41 [-]: CALL      R4 3 2       ; R4 := R4(R5 to R6)
   42 [-]: TEST      R4 0         ; if R4 then goto 44 else goto 105
   43 [-]: JMP       61           ; PC += 61 (goto 105)
   44 [-]: LOADK     R5 K9        ; R5 := Unknown_Type_Error
   45 [-]: GETGLOBAL R6 K10       ; R6 := ipairs
   46 [-]: MOVE      R7 R0        ; R7 := R0
   47 [-]: CALL      R6 2 4       ; R6 to R8 := R6(R7)
   48 [-]: JMP       11           ; PC += 11 (goto 60)
   49 [-]: GETUPVAL  R11 U1       ; R11 := U1
   50 [-]: GETTABLE  R11 R11 K7   ; R11 := R11["stat"]
   51 [-]: MOVE      R12 R10      ; R12 := R10
   52 [-]: LOADK     R13 K8       ; R13 := "mtime"
   53 [-]: CALL      R11 3 2      ; R11 := R11(R12 to R13)
   54 [-]: TEST      R11 0        ; if R11 then goto 56 else goto 60
   55 [-]: JMP       4            ; PC += 4 (goto 60)
   56 [-]: LT        0 R5 R11     ; if R5 < R11 then goto 58 else goto 60
   57 [-]: JMP       2            ; PC += 2 (goto 60)
   58 [-]: TESTSET   R5 R11 1     ; if R11 then R5 := R11 ; goto 60 else goto 60
   59 [-]: JMP       0            ; PC += 0 (goto 60)
   60 [-]: TFORLOOP  R6 2         ; R9 to R10 := R6(R7,R8); if R9 ~= nil then R8 := R9 else goto 62
   61 [-]: JMP       -13          ; PC += -13 (goto 49)
   62 [-]: LT        0 R5 R4      ; if R5 < R4 then goto 64 else goto 105
   63 [-]: JMP       41           ; PC += 41 (goto 105)
   64 [-]: GETUPVAL  R6 U2        ; R6 := U2
   65 [-]: GETTABLE  R6 R6 K11    ; R6 := R6["process"]
   66 [-]: GETTABLE  R6 R6 K12    ; R6 := R6["info"]
   67 [-]: LOADK     R7 K13       ; R7 := "uid"
   68 [-]: CALL      R6 2 2       ; R6 := R6(R7)
   69 [-]: EQ        0 R6 K9      ; if R6 == Unknown_Type_Error then goto 71 else goto 105
   70 [-]: JMP       34           ; PC += 34 (goto 105)
   71 [-]: GETGLOBAL R6 K14       ; R6 := assert
   72 [-]: GETUPVAL  R7 U2        ; R7 := U2
   73 [-]: GETTABLE  R7 R7 K11    ; R7 := R7["process"]
   74 [-]: GETTABLE  R7 R7 K12    ; R7 := R7["info"]
   75 [-]: LOADK     R8 K13       ; R8 := "uid"
   76 [-]: CALL      R7 2 2       ; R7 := R7(R8)
   77 [-]: GETUPVAL  R8 U1        ; R8 := U1
   78 [-]: GETTABLE  R8 R8 K7     ; R8 := R8["stat"]
   79 [-]: GETGLOBAL R9 K6        ; R9 := indexcache
   80 [-]: LOADK     R10 K13      ; R10 := "uid"
   81 [-]: CALL      R8 3 2       ; R8 := R8(R9 to R10)
   82 [-]: EQ        0 R7 R8      ; if R7 == R8 then goto 84 else goto 91
   83 [-]: JMP       7            ; PC += 7 (goto 91)
   84 [-]: GETUPVAL  R7 U1        ; R7 := U1
   85 [-]: GETTABLE  R7 R7 K7     ; R7 := R7["stat"]
   86 [-]: GETGLOBAL R8 K6        ; R8 := indexcache
   87 [-]: LOADK     R9 K15       ; R9 := "modestr"
   88 [-]: CALL      R7 3 2       ; R7 := R7(R8 to R9)
   89 [-]: EQ        1 R7 K16     ; if R7 ~= "rw-------" then goto 91 else goto 92
   90 [-]: JMP       1            ; PC += 1 (goto 92)
   91 [-]: LOADBOOL  R7 0 1       ; R7 := false; goto 93
   92 [-]: LOADBOOL  R7 1 0       ; R7 := true
   93 [-]: LOADK     R8 K17       ; R8 := "Fatal: Indexcache is not sane!"
   94 [-]: CALL      R6 3 1       ;  := R6(R7 to R8)
   95 [-]: GETGLOBAL R6 K18       ; R6 := loadfile
   96 [-]: GETGLOBAL R7 K6        ; R7 := indexcache
   97 [-]: CALL      R6 2 2       ; R6 := R6(R7)
   98 [-]: CALL      R6 1 2       ; R6 := R6()
   99 [-]: SETUPVAL  R6 U3        ; U3 := R6
  100 [-]: GETUPVAL  R6 U3        ; R6 := U3
  101 [-]: TEST      R6 0         ; if R6 then goto 103 else goto 105
  102 [-]: JMP       2            ; PC += 2 (goto 105)
  103 [-]: GETUPVAL  R6 U3        ; R6 := U3
  104 [-]: RETURN    R6 2         ; return R6
  105 [-]: NEWTABLE  R4 0 0       ; R4 := {} (size = 0,0)
  106 [-]: SETUPVAL  R4 U3        ; U3 := R4
  107 [-]: GETGLOBAL R4 K10       ; R4 := ipairs
  108 [-]: MOVE      R5 R0        ; R5 := R0
  109 [-]: CALL      R4 2 4       ; R4 to R6 := R4(R5)
  110 [-]: JMP       48           ; PC += 48 (goto 159)
  111 [-]: LOADK     R9 K19       ; R9 := "luci.controller."
  112 [-]: SELF      R10 R8 K20   ; R11 := R8; R10 := R8["sub"]
  113 [-]: LEN       R12 R1       ; R12 := #R1
  114 [-]: ADD       R12 R12 K4   ; R12 := R12 + Unknown_Type_Error
  115 [-]: LEN       R13 R8       ; R13 := #R8
  116 [-]: SUB       R13 R13 K21  ; R13 := R13 - Unknown_Type_Error
  117 [-]: CALL      R10 4 2      ; R10 := R10(R11 to R13)
  118 [-]: SELF      R10 R10 K22  ; R11 := R10; R10 := R10["gsub"]
  119 [-]: LOADK     R12 K23      ; R12 := "/"
  120 [-]: LOADK     R13 K24      ; R13 := "."
  121 [-]: CALL      R10 4 2      ; R10 := R10(R11 to R13)
  122 [-]: CONCAT    R9 R9 R10    ; R9 := concat(R9 to R10)
  123 [-]: GETGLOBAL R10 K25      ; R10 := require
  124 [-]: MOVE      R11 R9       ; R11 := R9
  125 [-]: CALL      R10 2 2      ; R10 := R10(R11)
  126 [-]: GETGLOBAL R11 K14      ; R11 := assert
  127 [-]: EQ        0 R10 K26    ; if R10 == true then goto 129 else goto 130
  128 [-]: JMP       1            ; PC += 1 (goto 130)
  129 [-]: LOADBOOL  R12 0 1      ; R12 := false; goto 131
  130 [-]: LOADBOOL  R12 1 0      ; R12 := true
  131 [-]: LOADK     R13 K27      ; R13 := "Invalid controller file found\n"
  132 [-]: LOADK     R14 K28      ; R14 := "The file \'"
  133 [-]: MOVE      R15 R8       ; R15 := R8
  134 [-]: LOADK     R16 K29      ; R16 := "\' contains an invalid module line.\n"
  135 [-]: LOADK     R17 K30      ; R17 := "Please verify whether the module name is set to \'"
  136 [-]: MOVE      R18 R9       ; R18 := R9
  137 [-]: LOADK     R19 K31      ; R19 := "\' - It must correspond to the file path!"
  138 [-]: CONCAT    R13 R13 R19  ; R13 := concat(R13 to R19)
  139 [-]: CALL      R11 3 1      ;  := R11(R12 to R13)
  140 [-]: GETTABLE  R11 R10 K32  ; R11 := R10["index"]
  141 [-]: GETGLOBAL R12 K14      ; R12 := assert
  142 [-]: GETGLOBAL R13 K33      ; R13 := type
  143 [-]: MOVE      R14 R11      ; R14 := R11
  144 [-]: CALL      R13 2 2      ; R13 := R13(R14)
  145 [-]: EQ        1 R13 K34    ; if R13 ~= "function" then goto 147 else goto 148
  146 [-]: JMP       1            ; PC += 1 (goto 148)
  147 [-]: LOADBOOL  R13 0 1      ; R13 := false; goto 149
  148 [-]: LOADBOOL  R13 1 0      ; R13 := true
  149 [-]: LOADK     R14 K27      ; R14 := "Invalid controller file found\n"
  150 [-]: LOADK     R15 K28      ; R15 := "The file \'"
  151 [-]: MOVE      R16 R8       ; R16 := R8
  152 [-]: LOADK     R17 K35      ; R17 := "\' contains no index() function.\n"
  153 [-]: LOADK     R18 K36      ; R18 := "Please make sure that the controller contains a valid "
  154 [-]: LOADK     R19 K37      ; R19 := "index function and verify the spelling!"
  155 [-]: CONCAT    R14 R14 R19  ; R14 := concat(R14 to R19)
  156 [-]: CALL      R12 3 1      ;  := R12(R13 to R14)
  157 [-]: GETUPVAL  R12 U3       ; R12 := U3
  158 [-]: SETTABLE  R12 R9 R11   ; R12[R9] := R11
  159 [-]: TFORLOOP  R4 2         ; R7 to R8 := R4(R5,R6); if R7 ~= nil then R6 := R7 else goto 161
  160 [-]: JMP       -50          ; PC += -50 (goto 111)
  161 [-]: GETGLOBAL R4 K6        ; R4 := indexcache
  162 [-]: TEST      R4 0         ; if R4 then goto 164 else goto 178
  163 [-]: JMP       14           ; PC += 14 (goto 178)
  164 [-]: GETUPVAL  R4 U4        ; R4 := U4
  165 [-]: GETTABLE  R4 R4 K38    ; R4 := R4["open"]
  166 [-]: GETGLOBAL R5 K6        ; R5 := indexcache
  167 [-]: LOADK     R6 K39       ; R6 := "w"
  168 [-]: LOADK     R7 K40       ; R7 := Unknown_Type_Error
  169 [-]: CALL      R4 4 2       ; R4 := R4(R5 to R7)
  170 [-]: SELF      R5 R4 K41    ; R6 := R4; R5 := R4["writeall"]
  171 [-]: GETUPVAL  R7 U0        ; R7 := U0
  172 [-]: GETTABLE  R7 R7 K42    ; R7 := R7["get_bytecode"]
  173 [-]: GETUPVAL  R8 U3        ; R8 := U3
  174 [-]: CALL      R7 2 0       ; R7 to top := R7(R8)
  175 [-]: CALL      R5 0 1       ;  := R5(R6 to top)
  176 [-]: SELF      R5 R4 K43    ; R6 := R4; R5 := R4["close"]
  177 [-]: CALL      R5 2 1       ;  := R5(R6)
  178 [-]: RETURN    R0 1         ; return 


; Function:        0_20_0
; Defined at line: 793
; #Upvalues:       0
; #Parameters:     0
; Is_vararg:       0
; Max Stack Size:  2

    0 [-]: RETURN    R0 1         ; return 


; Function:        0_20_1
; Defined at line: 797
; #Upvalues:       0
; #Parameters:     0
; Is_vararg:       0
; Max Stack Size:  2

    0 [-]: RETURN    R0 1         ; return 


; Function:        0_21
; Defined at line: 854
; #Upvalues:       2
; #Parameters:     0
; Is_vararg:       0
; Max Stack Size:  13

    0 [-]: GETUPVAL  R0 U0        ; R0 := U0
    1 [-]: TEST      R0 1         ; if not R0 then goto 3 else goto 5
    2 [-]: JMP       2            ; PC += 2 (goto 5)
    3 [-]: GETGLOBAL R0 K0        ; R0 := createindex
    4 [-]: CALL      R0 1 1       ;  := R0()
    5 [-]: GETGLOBAL R0 K1        ; R0 := context
    6 [-]: NEWTABLE  R1 0 2       ; R1 := {} (size = 0,2)
    7 [-]: NEWTABLE  R2 0 0       ; R2 := {} (size = 0,0)
    8 [-]: SETTABLE  R1 K2 R2     ; R1["nodes"] := R2
    9 [-]: SETTABLE  R1 K3 K4     ; R1["inreq"] := true
   10 [-]: NEWTABLE  R2 0 0       ; R2 := {} (size = 0,0)
   11 [-]: GETGLOBAL R3 K6        ; R3 := setmetatable
   12 [-]: NEWTABLE  R4 0 0       ; R4 := {} (size = 0,0)
   13 [-]: NEWTABLE  R5 0 1       ; R5 := {} (size = 0,1)
   14 [-]: SETTABLE  R5 K7 K8     ; R5["__mode"] := "v"
   15 [-]: CALL      R3 3 2       ; R3 := R3(R4 to R5)
   16 [-]: SETTABLE  R0 K5 R3     ; R0["treecache"] := R3
   17 [-]: SETTABLE  R0 K9 R1     ; R0["tree"] := R1
   18 [-]: SETTABLE  R0 K10 R2    ; R0["modifiers"] := R2
   19 [-]: GETGLOBAL R3 K11       ; R3 := require
   20 [-]: LOADK     R4 K12       ; R4 := "luci.i18n"
   21 [-]: CALL      R3 2 2       ; R3 := R3(R4)
   22 [-]: GETTABLE  R3 R3 K13    ; R3 := R3["loadc"]
   23 [-]: LOADK     R4 K14       ; R4 := "base"
   24 [-]: CALL      R3 2 1       ;  := R3(R4)
   25 [-]: GETGLOBAL R3 K6        ; R3 := setmetatable
   26 [-]: NEWTABLE  R4 0 0       ; R4 := {} (size = 0,0)
   27 [-]: NEWTABLE  R5 0 1       ; R5 := {} (size = 0,1)
   28 [-]: GETGLOBAL R6 K16       ; R6 := luci
   29 [-]: GETTABLE  R6 R6 K17    ; R6 := R6["dispatcher"]
   30 [-]: SETTABLE  R5 K15 R6    ; R5["__index"] := R6
   31 [-]: CALL      R3 3 2       ; R3 := R3(R4 to R5)
   32 [-]: GETGLOBAL R4 K18       ; R4 := pairs
   33 [-]: GETUPVAL  R5 U0        ; R5 := U0
   34 [-]: CALL      R4 2 4       ; R4 to R6 := R4(R5)
   35 [-]: JMP       7            ; PC += 7 (goto 43)
   36 [-]: SETTABLE  R3 K19 R7    ; R3["_NAME"] := R7
   37 [-]: GETGLOBAL R9 K20       ; R9 := setfenv
   38 [-]: MOVE      R10 R8       ; R10 := R8
   39 [-]: MOVE      R11 R3       ; R11 := R3
   40 [-]: CALL      R9 3 1       ;  := R9(R10 to R11)
   41 [-]: MOVE      R9 R8        ; R9 := R8
   42 [-]: CALL      R9 1 1       ;  := R9()
   43 [-]: TFORLOOP  R4 2         ; R7 to R8 := R4(R5,R6); if R7 ~= nil then R6 := R7 else goto 45
   44 [-]: JMP       -9           ; PC += -9 (goto 36)
   45 [-]: CLOSURE   R4 0         ; R4 := closure(Function #0_21_0)
   46 [-]: MOVE      R0 R2        ; R0 := R2
   47 [-]: GETUPVAL  R5 U1        ; R5 := U1
   48 [-]: GETTABLE  R5 R5 K21    ; R5 := R5["spairs"]
   49 [-]: MOVE      R6 R2        ; R6 := R2
   50 [-]: MOVE      R7 R4        ; R7 := R4
   51 [-]: CALL      R5 3 4       ; R5 to R7 := R5(R6 to R7)
   52 [-]: JMP       8            ; PC += 8 (goto 61)
   53 [-]: GETTABLE  R10 R9 K22   ; R10 := R9["module"]
   54 [-]: SETTABLE  R3 K19 R10   ; R3["_NAME"] := R10
   55 [-]: GETGLOBAL R10 K20      ; R10 := setfenv
   56 [-]: GETTABLE  R11 R9 K23   ; R11 := R9["func"]
   57 [-]: MOVE      R12 R3       ; R12 := R3
   58 [-]: CALL      R10 3 1      ;  := R10(R11 to R12)
   59 [-]: GETTABLE  R10 R9 K23   ; R10 := R9["func"]
   60 [-]: CALL      R10 1 1      ;  := R10()
   61 [-]: TFORLOOP  R5 2         ; R8 to R9 := R5(R6,R7); if R8 ~= nil then R7 := R8 else goto 63
   62 [-]: JMP       -10          ; PC += -10 (goto 53)
   63 [-]: RETURN    R1 2         ; return R1
   64 [-]: RETURN    R0 1         ; return 


; Function:        0_21_0
; Defined at line: 878
; #Upvalues:       1
; #Parameters:     2
; Is_vararg:       0
; Max Stack Size:  4

    0 [-]: GETUPVAL  R2 U0        ; R2 := U0
    1 [-]: GETTABLE  R2 R2 R0     ; R2 := R2[R0]
    2 [-]: GETTABLE  R2 R2 K0     ; R2 := R2["order"]
    3 [-]: GETUPVAL  R3 U0        ; R3 := U0
    4 [-]: GETTABLE  R3 R3 R1     ; R3 := R3[R1]
    5 [-]: GETTABLE  R3 R3 K0     ; R3 := R3["order"]
    6 [-]: LT        1 R2 R3      ; if R2 >= R3 then goto 8 else goto 9
    7 [-]: JMP       1            ; PC += 1 (goto 9)
    8 [-]: LOADBOOL  R2 0 1       ; R2 := false; goto 10
    9 [-]: LOADBOOL  R2 1 0       ; R2 := true
   10 [-]: RETURN    R2 2         ; return R2
   11 [-]: RETURN    R0 1         ; return 


; Function:        0_22
; Defined at line: 891
; #Upvalues:       0
; #Parameters:     2
; Is_vararg:       0
; Max Stack Size:  7

    0 [-]: GETGLOBAL R2 K0        ; R2 := context
    1 [-]: GETTABLE  R2 R2 K1     ; R2 := R2["modifiers"]
    2 [-]: GETGLOBAL R3 K0        ; R3 := context
    3 [-]: GETTABLE  R3 R3 K1     ; R3 := R3["modifiers"]
    4 [-]: LEN       R3 R3        ; R3 := #R3
    5 [-]: ADD       R3 R3 K2     ; R3 := R3 + Unknown_Type_Error
    6 [-]: NEWTABLE  R4 0 3       ; R4 := {} (size = 0,3)
    7 [-]: SETTABLE  R4 K3 R0     ; R4["func"] := R0
    8 [-]: TESTSET   R5 R1 1      ; if R1 then R5 := R1 ; goto 11 else goto 10
    9 [-]: JMP       1            ; PC += 1 (goto 11)
   10 [-]: LOADK     R5 K5        ; R5 := Unknown_Type_Error
   11 [-]: SETTABLE  R4 K4 R5     ; R4["order"] := R5
   12 [-]: GETGLOBAL R5 K7        ; R5 := getfenv
   13 [-]: LOADK     R6 K8        ; R6 := Unknown_Type_Error
   14 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   15 [-]: GETTABLE  R5 R5 K9     ; R5 := R5["_NAME"]
   16 [-]: SETTABLE  R4 K6 R5     ; R4["module"] := R5
   17 [-]: SETTABLE  R2 R3 R4     ; R2[R3] := R4
   18 [-]: RETURN    R0 1         ; return 


; Function:        0_23
; Defined at line: 900
; #Upvalues:       0
; #Parameters:     4
; Is_vararg:       0
; Max Stack Size:  10

    0 [-]: GETGLOBAL R4 K0        ; R4 := node
    1 [-]: GETGLOBAL R5 K1        ; R5 := unpack
    2 [-]: MOVE      R6 R0        ; R6 := R0
    3 [-]: CALL      R5 2 0       ; R5 to top := R5(R6)
    4 [-]: CALL      R4 0 2       ; R4 := R4(R5 to top)
    5 [-]: SETTABLE  R4 K2 K3     ; R4["nodes"] := nil
    6 [-]: SETTABLE  R4 K4 K3     ; R4["module"] := nil
    7 [-]: SETTABLE  R4 K5 R2     ; R4["title"] := R2
    8 [-]: SETTABLE  R4 K6 R3     ; R4["order"] := R3
    9 [-]: GETGLOBAL R5 K7        ; R5 := setmetatable
   10 [-]: MOVE      R6 R4        ; R6 := R4
   11 [-]: NEWTABLE  R7 0 1       ; R7 := {} (size = 0,1)
   12 [-]: GETGLOBAL R8 K9        ; R8 := _create_node
   13 [-]: MOVE      R9 R1        ; R9 := R1
   14 [-]: CALL      R8 2 2       ; R8 := R8(R9)
   15 [-]: SETTABLE  R7 K8 R8     ; R7["__index"] := R8
   16 [-]: CALL      R5 3 1       ;  := R5(R6 to R7)
   17 [-]: RETURN    R4 2         ; return R4
   18 [-]: RETURN    R0 1         ; return 


; Function:        0_24
; Defined at line: 913
; #Upvalues:       0
; #Parameters:     4
; Is_vararg:       0
; Max Stack Size:  7

    0 [-]: GETGLOBAL R4 K0        ; R4 := node
    1 [-]: GETGLOBAL R5 K1        ; R5 := unpack
    2 [-]: MOVE      R6 R0        ; R6 := R0
    3 [-]: CALL      R5 2 0       ; R5 to top := R5(R6)
    4 [-]: CALL      R4 0 2       ; R4 := R4(R5 to top)
    5 [-]: SETTABLE  R4 K2 R1     ; R4["target"] := R1
    6 [-]: SETTABLE  R4 K3 R2     ; R4["title"] := R2
    7 [-]: SETTABLE  R4 K4 R3     ; R4["order"] := R3
    8 [-]: GETGLOBAL R5 K6        ; R5 := getfenv
    9 [-]: LOADK     R6 K7        ; R6 := Unknown_Type_Error
   10 [-]: CALL      R5 2 2       ; R5 := R5(R6)
   11 [-]: GETTABLE  R5 R5 K8     ; R5 := R5["_NAME"]
   12 [-]: SETTABLE  R4 K5 R5     ; R4["module"] := R5
   13 [-]: RETURN    R4 2         ; return R4
   14 [-]: RETURN    R0 1         ; return 


; Function:        0_25
; Defined at line: 925
; #Upvalues:       0
; #Parameters:     0
; Is_vararg:       3
; Max Stack Size:  4

    0 [-]: GETGLOBAL R1 K0        ; R1 := _create_node
    1 [-]: NEWTABLE  R2 0 0       ; R2 := {} (size = 0,0)
    2 [-]: VARARG    R3 0         ; R3 to top := ...
    3 [-]: SETLIST   R2 0 1       ; R2[0] to R2[top] := R3 to top ; R(a)[(c-1)*FPF+i] := R(a+i), 1 <= i <= b, a=2, b=0, c=1, FPF=50
    4 [-]: TAILCALL  R1 2 0       ; R1 to top := R1(R2)
    5 [-]: RETURN    R1 0         ; return R1 to top
    6 [-]: RETURN    R0 1         ; return 


; Function:        0_26
; Defined at line: 929
; #Upvalues:       0
; #Parameters:     0
; Is_vararg:       3
; Max Stack Size:  4

    0 [-]: GETGLOBAL R1 K0        ; R1 := _create_node
    1 [-]: NEWTABLE  R2 0 0       ; R2 := {} (size = 0,0)
    2 [-]: VARARG    R3 0         ; R3 to top := ...
    3 [-]: SETLIST   R2 0 1       ; R2[0] to R2[top] := R3 to top ; R(a)[(c-1)*FPF+i] := R(a+i), 1 <= i <= b, a=2, b=0, c=1, FPF=50
    4 [-]: CALL      R1 2 2       ; R1 := R1(R2)
    5 [-]: GETGLOBAL R2 K2        ; R2 := getfenv
    6 [-]: LOADK     R3 K3        ; R3 := Unknown_Type_Error
    7 [-]: CALL      R2 2 2       ; R2 := R2(R3)
    8 [-]: GETTABLE  R2 R2 K4     ; R2 := R2["_NAME"]
    9 [-]: SETTABLE  R1 K1 R2     ; R1["module"] := R2
   10 [-]: SETTABLE  R1 K5 K6     ; R1["auto"] := nil
   11 [-]: RETURN    R1 2         ; return R1
   12 [-]: RETURN    R0 1         ; return 


; Function:        0_27
; Defined at line: 938
; #Upvalues:       0
; #Parameters:     1
; Is_vararg:       0
; Max Stack Size:  7

    0 [-]: LEN       R1 R0        ; R1 := #R0
    1 [-]: EQ        0 R1 K0      ; if R1 == Unknown_Type_Error then goto 3 else goto 6
    2 [-]: JMP       3            ; PC += 3 (goto 6)
    3 [-]: GETGLOBAL R1 K1        ; R1 := context
    4 [-]: GETTABLE  R1 R1 K2     ; R1 := R1["tree"]
    5 [-]: RETURN    R1 2         ; return R1
    6 [-]: GETGLOBAL R1 K3        ; R1 := table
    7 [-]: GETTABLE  R1 R1 K4     ; R1 := R1["concat"]
    8 [-]: MOVE      R2 R0        ; R2 := R0
    9 [-]: LOADK     R3 K5        ; R3 := "."
   10 [-]: CALL      R1 3 2       ; R1 := R1(R2 to R3)
   11 [-]: GETGLOBAL R2 K1        ; R2 := context
   12 [-]: GETTABLE  R2 R2 K6     ; R2 := R2["treecache"]
   13 [-]: GETTABLE  R2 R2 R1     ; R2 := R2[R1]
   14 [-]: TEST      R2 1         ; if not R2 then goto 16 else goto 44
   15 [-]: JMP       28           ; PC += 28 (goto 44)
   16 [-]: GETGLOBAL R3 K3        ; R3 := table
   17 [-]: GETTABLE  R3 R3 K7     ; R3 := R3["remove"]
   18 [-]: MOVE      R4 R0        ; R4 := R0
   19 [-]: CALL      R3 2 2       ; R3 := R3(R4)
   20 [-]: GETGLOBAL R4 K8        ; R4 := _create_node
   21 [-]: MOVE      R5 R0        ; R5 := R0
   22 [-]: CALL      R4 2 2       ; R4 := R4(R5)
   23 [-]: NEWTABLE  R5 0 2       ; R5 := {} (size = 0,2)
   24 [-]: NEWTABLE  R6 0 0       ; R6 := {} (size = 0,0)
   25 [-]: SETTABLE  R5 K9 R6     ; R5["nodes"] := R6
   26 [-]: SETTABLE  R5 K10 K11   ; R5["auto"] := true
   27 [-]: MOVE      R2 R5        ; R2 := R5
   28 [-]: GETTABLE  R5 R4 K12    ; R5 := R4["inreq"]
   29 [-]: TEST      R5 0         ; if R5 then goto 31 else goto 39
   30 [-]: JMP       8            ; PC += 8 (goto 39)
   31 [-]: GETGLOBAL R5 K1        ; R5 := context
   32 [-]: GETTABLE  R5 R5 K13    ; R5 := R5["path"]
   33 [-]: LEN       R6 R0        ; R6 := #R0
   34 [-]: ADD       R6 R6 K14    ; R6 := R6 + Unknown_Type_Error
   35 [-]: GETTABLE  R5 R5 R6     ; R5 := R5[R6]
   36 [-]: EQ        0 R5 R3      ; if R5 == R3 then goto 38 else goto 39
   37 [-]: JMP       1            ; PC += 1 (goto 39)
   38 [-]: SETTABLE  R2 K12 K11   ; R2["inreq"] := true
   39 [-]: GETTABLE  R5 R4 K9     ; R5 := R4["nodes"]
   40 [-]: SETTABLE  R5 R3 R2     ; R5[R3] := R2
   41 [-]: GETGLOBAL R5 K1        ; R5 := context
   42 [-]: GETTABLE  R5 R5 K6     ; R5 := R5["treecache"]
   43 [-]: SETTABLE  R5 R1 R2     ; R5[R1] := R2
   44 [-]: RETURN    R2 2         ; return R2
   45 [-]: RETURN    R0 1         ; return 


; Function:        0_28
; Defined at line: 964
; #Upvalues:       0
; #Parameters:     0
; Is_vararg:       0
; Max Stack Size:  13

    0 [-]: NEWTABLE  R0 0 0       ; R0 := {} (size = 0,0)
    1 [-]: GETGLOBAL R1 K0        ; R1 := unpack
    2 [-]: GETGLOBAL R2 K1        ; R2 := context
    3 [-]: GETTABLE  R2 R2 K2     ; R2 := R2["path"]
    4 [-]: CALL      R1 2 0       ; R1 to top := R1(R2)
    5 [-]: SETLIST   R0 0 1       ; R0[0] to R0[top] := R1 to top ; R(a)[(c-1)*FPF+i] := R(a+i), 1 <= i <= b, a=0, b=0, c=1, FPF=50
    6 [-]: GETGLOBAL R1 K3        ; R1 := table
    7 [-]: GETTABLE  R1 R1 K4     ; R1 := R1["concat"]
    8 [-]: MOVE      R2 R0        ; R2 := R0
    9 [-]: LOADK     R3 K5        ; R3 := "."
   10 [-]: CALL      R1 3 2       ; R1 := R1(R2 to R3)
   11 [-]: GETGLOBAL R2 K1        ; R2 := context
   12 [-]: GETTABLE  R2 R2 K6     ; R2 := R2["treecache"]
   13 [-]: GETTABLE  R2 R2 R1     ; R2 := R2[R1]
   14 [-]: LOADNIL   R3 R3        ; R3 := nil
   15 [-]: TEST      R2 0         ; if R2 then goto 17 else goto 47
   16 [-]: JMP       30           ; PC += 30 (goto 47)
   17 [-]: GETTABLE  R4 R2 K7     ; R4 := R2["nodes"]
   18 [-]: TEST      R4 0         ; if R4 then goto 20 else goto 47
   19 [-]: JMP       27           ; PC += 27 (goto 47)
   20 [-]: GETGLOBAL R4 K8        ; R4 := next
   21 [-]: GETTABLE  R5 R2 K7     ; R5 := R2["nodes"]
   22 [-]: CALL      R4 2 2       ; R4 := R4(R5)
   23 [-]: TEST      R4 0         ; if R4 then goto 25 else goto 47
   24 [-]: JMP       22           ; PC += 22 (goto 47)
   25 [-]: LOADNIL   R4 R5        ; R4 to R5 := nil
   26 [-]: GETGLOBAL R6 K9        ; R6 := pairs
   27 [-]: GETTABLE  R7 R2 K7     ; R7 := R2["nodes"]
   28 [-]: CALL      R6 2 4       ; R6 to R8 := R6(R7)
   29 [-]: JMP       15           ; PC += 15 (goto 45)
   30 [-]: TEST      R3 0         ; if R3 then goto 32 else goto 44
   31 [-]: JMP       12           ; PC += 12 (goto 44)
   32 [-]: GETTABLE  R11 R10 K10  ; R11 := R10["order"]
   33 [-]: TEST      R11 1        ; if not R11 then goto 35 else goto 36
   34 [-]: JMP       1            ; PC += 1 (goto 36)
   35 [-]: LOADK     R11 K11      ; R11 := Unknown_Type_Error
   36 [-]: GETTABLE  R12 R2 K7    ; R12 := R2["nodes"]
   37 [-]: GETTABLE  R12 R12 R3   ; R12 := R12[R3]
   38 [-]: GETTABLE  R12 R12 K10  ; R12 := R12["order"]
   39 [-]: TEST      R12 1        ; if not R12 then goto 41 else goto 42
   40 [-]: JMP       1            ; PC += 1 (goto 42)
   41 [-]: LOADK     R12 K11      ; R12 := Unknown_Type_Error
   42 [-]: LT        0 R11 R12    ; if R11 < R12 then goto 44 else goto 45
   43 [-]: JMP       1            ; PC += 1 (goto 45)
   44 [-]: MOVE      R3 R9        ; R3 := R9
   45 [-]: TFORLOOP  R6 2         ; R9 to R10 := R6(R7,R8); if R9 ~= nil then R8 := R9 else goto 47
   46 [-]: JMP       -17          ; PC += -17 (goto 30)
   47 [-]: GETGLOBAL R4 K12       ; R4 := assert
   48 [-]: EQ        0 R3 K13     ; if R3 == nil then goto 50 else goto 51
   49 [-]: JMP       1            ; PC += 1 (goto 51)
   50 [-]: LOADBOOL  R5 0 1       ; R5 := false; goto 52
   51 [-]: LOADBOOL  R5 1 0       ; R5 := true
   52 [-]: LOADK     R6 K14       ; R6 := "The requested node contains no childs, unable to redispatch"
   53 [-]: CALL      R4 3 1       ;  := R4(R5 to R6)
   54 [-]: LEN       R4 R0        ; R4 := #R0
   55 [-]: ADD       R4 R4 K15    ; R4 := R4 + Unknown_Type_Error
   56 [-]: SETTABLE  R0 R4 R3     ; R0[R4] := R3
   57 [-]: GETGLOBAL R4 K16       ; R4 := dispatch
   58 [-]: MOVE      R5 R0        ; R5 := R0
   59 [-]: CALL      R4 2 1       ;  := R4(R5)
   60 [-]: RETURN    R0 1         ; return 


; Function:        0_29
; Defined at line: 988
; #Upvalues:       0
; #Parameters:     0
; Is_vararg:       0
; Max Stack Size:  2

    0 [-]: NEWTABLE  R0 0 2       ; R0 := {} (size = 0,2)
    1 [-]: SETTABLE  R0 K0 K1     ; R0["type"] := "firstchild"
    2 [-]: GETGLOBAL R1 K3        ; R1 := _firstchild
    3 [-]: SETTABLE  R0 K2 R1     ; R0["target"] := R1
    4 [-]: RETURN    R0 2         ; return R0
    5 [-]: RETURN    R0 1         ; return 


; Function:        0_30
; Defined at line: 992
; #Upvalues:       0
; #Parameters:     0
; Is_vararg:       3
; Max Stack Size:  3

    0 [-]: NEWTABLE  R1 0 0       ; R1 := {} (size = 0,0)
    1 [-]: VARARG    R2 0         ; R2 to top := ...
    2 [-]: SETLIST   R1 0 1       ; R1[0] to R1[top] := R2 to top ; R(a)[(c-1)*FPF+i] := R(a+i), 1 <= i <= b, a=1, b=0, c=1, FPF=50
    3 [-]: CLOSURE   R2 0         ; R2 := closure(Function #0_30_0)
    4 [-]: MOVE      R0 R1        ; R0 := R1
    5 [-]: RETURN    R2 2         ; return R2
    6 [-]: RETURN    R0 1         ; return 


; Function:        0_30_0
; Defined at line: 994
; #Upvalues:       1
; #Parameters:     0
; Is_vararg:       3
; Max Stack Size:  8

    0 [-]: GETGLOBAL R1 K0        ; R1 := ipairs
    1 [-]: NEWTABLE  R2 0 0       ; R2 := {} (size = 0,0)
    2 [-]: VARARG    R3 0         ; R3 to top := ...
    3 [-]: SETLIST   R2 0 1       ; R2[0] to R2[top] := R3 to top ; R(a)[(c-1)*FPF+i] := R(a+i), 1 <= i <= b, a=2, b=0, c=1, FPF=50
    4 [-]: CALL      R1 2 4       ; R1 to R3 := R1(R2)
    5 [-]: JMP       5            ; PC += 5 (goto 11)
    6 [-]: GETUPVAL  R6 U0        ; R6 := U0
    7 [-]: GETUPVAL  R7 U0        ; R7 := U0
    8 [-]: LEN       R7 R7        ; R7 := #R7
    9 [-]: ADD       R7 R7 K1     ; R7 := R7 + Unknown_Type_Error
   10 [-]: SETTABLE  R6 R7 R5     ; R6[R7] := R5
   11 [-]: TFORLOOP  R1 2         ; R4 to R5 := R1(R2,R3); if R4 ~= nil then R3 := R4 else goto 13
   12 [-]: JMP       -7           ; PC += -7 (goto 6)
   13 [-]: GETGLOBAL R1 K2        ; R1 := dispatch
   14 [-]: GETUPVAL  R2 U0        ; R2 := U0
   15 [-]: CALL      R1 2 1       ;  := R1(R2)
   16 [-]: RETURN    R0 1         ; return 


; Function:        0_31
; Defined at line: 1003
; #Upvalues:       1
; #Parameters:     1
; Is_vararg:       3
; Max Stack Size:  4

    0 [-]: NEWTABLE  R2 0 0       ; R2 := {} (size = 0,0)
    1 [-]: VARARG    R3 0         ; R3 to top := ...
    2 [-]: SETLIST   R2 0 1       ; R2[0] to R2[top] := R3 to top ; R(a)[(c-1)*FPF+i] := R(a+i), 1 <= i <= b, a=2, b=0, c=1, FPF=50
    3 [-]: CLOSURE   R3 0         ; R3 := closure(Function #0_31_0)
    4 [-]: GETUPVAL  R0 U0        ; R0 := U0
    5 [-]: MOVE      R0 R0        ; R0 := R0
    6 [-]: MOVE      R0 R2        ; R0 := R2
    7 [-]: RETURN    R3 2         ; return R3
    8 [-]: RETURN    R0 1         ; return 


; Function:        0_31_0
; Defined at line: 1005
; #Upvalues:       3
; #Parameters:     0
; Is_vararg:       3
; Max Stack Size:  11

    0 [-]: GETUPVAL  R1 U0        ; R1 := U0
    1 [-]: GETTABLE  R1 R1 K0     ; R1 := R1["clone"]
    2 [-]: GETGLOBAL R2 K1        ; R2 := context
    3 [-]: GETTABLE  R2 R2 K2     ; R2 := R2["dispatched"]
    4 [-]: CALL      R1 2 2       ; R1 := R1(R2)
    5 [-]: LOADK     R2 K3        ; R2 := Unknown_Type_Error
    6 [-]: GETUPVAL  R3 U1        ; R3 := U1
    7 [-]: LOADK     R4 K3        ; R4 := Unknown_Type_Error
    8 [-]: FORPREP   R2 5         ; R2 -= R4; pc += 5 (goto 14)
    9 [-]: GETGLOBAL R6 K4        ; R6 := table
   10 [-]: GETTABLE  R6 R6 K5     ; R6 := R6["remove"]
   11 [-]: MOVE      R7 R1        ; R7 := R1
   12 [-]: LOADK     R8 K3        ; R8 := Unknown_Type_Error
   13 [-]: CALL      R6 3 1       ;  := R6(R7 to R8)
   14 [-]: FORLOOP   R2 -6        ; R2 += R4; if R2 <= R3 then R5 := R2; PC += -6 , goto 9 end
   15 [-]: GETGLOBAL R2 K6        ; R2 := ipairs
   16 [-]: GETUPVAL  R3 U2        ; R3 := U2
   17 [-]: CALL      R2 2 4       ; R2 to R4 := R2(R3)
   18 [-]: JMP       6            ; PC += 6 (goto 25)
   19 [-]: GETGLOBAL R7 K4        ; R7 := table
   20 [-]: GETTABLE  R7 R7 K7     ; R7 := R7["insert"]
   21 [-]: MOVE      R8 R1        ; R8 := R1
   22 [-]: MOVE      R9 R5        ; R9 := R5
   23 [-]: MOVE      R10 R6       ; R10 := R6
   24 [-]: CALL      R7 4 1       ;  := R7(R8 to R10)
   25 [-]: TFORLOOP  R2 2         ; R5 to R6 := R2(R3,R4); if R5 ~= nil then R4 := R5 else goto 27
   26 [-]: JMP       -8           ; PC += -8 (goto 19)
   27 [-]: GETGLOBAL R2 K6        ; R2 := ipairs
   28 [-]: NEWTABLE  R3 0 0       ; R3 := {} (size = 0,0)
   29 [-]: VARARG    R4 0         ; R4 to top := ...
   30 [-]: SETLIST   R3 0 1       ; R3[0] to R3[top] := R4 to top ; R(a)[(c-1)*FPF+i] := R(a+i), 1 <= i <= b, a=3, b=0, c=1, FPF=50
   31 [-]: CALL      R2 2 4       ; R2 to R4 := R2(R3)
   32 [-]: JMP       3            ; PC += 3 (goto 36)
   33 [-]: LEN       R7 R1        ; R7 := #R1
   34 [-]: ADD       R7 R7 K3     ; R7 := R7 + Unknown_Type_Error
   35 [-]: SETTABLE  R1 R7 R6     ; R1[R7] := R6
   36 [-]: TFORLOOP  R2 2         ; R5 to R6 := R2(R3,R4); if R5 ~= nil then R4 := R5 else goto 38
   37 [-]: JMP       -5           ; PC += -5 (goto 33)
   38 [-]: GETGLOBAL R2 K8        ; R2 := dispatch
   39 [-]: MOVE      R3 R1        ; R3 := R1
   40 [-]: CALL      R2 2 1       ;  := R2(R3)
   41 [-]: RETURN    R0 1         ; return 


; Function:        0_32
; Defined at line: 1025
; #Upvalues:       0
; #Parameters:     1
; Is_vararg:       3
; Max Stack Size:  11

    0 [-]: GETGLOBAL R2 K0        ; R2 := getfenv
    1 [-]: CALL      R2 1 2       ; R2 := R2()
    2 [-]: GETTABLE  R3 R0 K1     ; R3 := R0["name"]
    3 [-]: GETTABLE  R2 R2 R3     ; R2 := R2[R3]
    4 [-]: GETGLOBAL R3 K2        ; R3 := assert
    5 [-]: EQ        0 R2 K3      ; if R2 == nil then goto 7 else goto 8
    6 [-]: JMP       1            ; PC += 1 (goto 8)
    7 [-]: LOADBOOL  R4 0 1       ; R4 := false; goto 9
    8 [-]: LOADBOOL  R4 1 0       ; R4 := true
    9 [-]: LOADK     R5 K4        ; R5 := "Cannot resolve function \""
   10 [-]: GETTABLE  R6 R0 K1     ; R6 := R0["name"]
   11 [-]: LOADK     R7 K5        ; R7 := "\". Is it misspelled or local?"
   12 [-]: CONCAT    R5 R5 R7     ; R5 := concat(R5 to R7)
   13 [-]: CALL      R3 3 1       ;  := R3(R4 to R5)
   14 [-]: GETGLOBAL R3 K2        ; R3 := assert
   15 [-]: GETGLOBAL R4 K6        ; R4 := type
   16 [-]: MOVE      R5 R2        ; R5 := R2
   17 [-]: CALL      R4 2 2       ; R4 := R4(R5)
   18 [-]: EQ        1 R4 K7      ; if R4 ~= "function" then goto 20 else goto 21
   19 [-]: JMP       1            ; PC += 1 (goto 21)
   20 [-]: LOADBOOL  R4 0 1       ; R4 := false; goto 22
   21 [-]: LOADBOOL  R4 1 0       ; R4 := true
   22 [-]: LOADK     R5 K8        ; R5 := "The symbol \""
   23 [-]: GETTABLE  R6 R0 K1     ; R6 := R0["name"]
   24 [-]: LOADK     R7 K9        ; R7 := "\" does not refer to a function but data "
   25 [-]: LOADK     R8 K10       ; R8 := "of type \""
   26 [-]: GETGLOBAL R9 K6        ; R9 := type
   27 [-]: MOVE      R10 R2       ; R10 := R2
   28 [-]: CALL      R9 2 2       ; R9 := R9(R10)
   29 [-]: LOADK     R10 K11      ; R10 := "\"."
   30 [-]: CONCAT    R5 R5 R10    ; R5 := concat(R5 to R10)
   31 [-]: CALL      R3 3 1       ;  := R3(R4 to R5)
   32 [-]: GETTABLE  R3 R0 K12    ; R3 := R0["argv"]
   33 [-]: LEN       R3 R3        ; R3 := #R3
   34 [-]: LT        0 K13 R3     ; if Unknown_Type_Error < R3 then goto 36 else goto 44
   35 [-]: JMP       8            ; PC += 8 (goto 44)
   36 [-]: MOVE      R3 R2        ; R3 := R2
   37 [-]: GETGLOBAL R4 K14       ; R4 := unpack
   38 [-]: GETTABLE  R5 R0 K12    ; R5 := R0["argv"]
   39 [-]: CALL      R4 2 2       ; R4 := R4(R5)
   40 [-]: VARARG    R5 0         ; R5 to top := ...
   41 [-]: TAILCALL  R3 0 0       ; R3 to top := R3(R4 to top)
   42 [-]: RETURN    R3 0         ; return R3 to top
   43 [-]: JMP       4            ; PC += 4 (goto 48)
   44 [-]: MOVE      R3 R2        ; R3 := R2
   45 [-]: VARARG    R4 0         ; R4 to top := ...
   46 [-]: TAILCALL  R3 0 0       ; R3 to top := R3(R4 to top)
   47 [-]: RETURN    R3 0         ; return R3 to top
   48 [-]: RETURN    R0 1         ; return 


; Function:        0_33
; Defined at line: 1041
; #Upvalues:       1
; #Parameters:     1
; Is_vararg:       3
; Max Stack Size:  5

    0 [-]: NEWTABLE  R2 0 4       ; R2 := {} (size = 0,4)
    1 [-]: SETTABLE  R2 K0 K1     ; R2["type"] := "call"
    2 [-]: NEWTABLE  R3 0 0       ; R3 := {} (size = 0,0)
    3 [-]: VARARG    R4 0         ; R4 to top := ...
    4 [-]: SETLIST   R3 0 1       ; R3[0] to R3[top] := R4 to top ; R(a)[(c-1)*FPF+i] := R(a+i), 1 <= i <= b, a=3, b=0, c=1, FPF=50
    5 [-]: SETTABLE  R2 K2 R3     ; R2["argv"] := R3
    6 [-]: SETTABLE  R2 K3 R0     ; R2["name"] := R0
    7 [-]: GETUPVAL  R3 U0        ; R3 := U0
    8 [-]: SETTABLE  R2 K4 R3     ; R2["target"] := R3
    9 [-]: RETURN    R2 2         ; return R2
   10 [-]: RETURN    R0 1         ; return 


; Function:        0_34
; Defined at line: 1045
; #Upvalues:       1
; #Parameters:     2
; Is_vararg:       3
; Max Stack Size:  6

    0 [-]: NEWTABLE  R3 0 5       ; R3 := {} (size = 0,5)
    1 [-]: SETTABLE  R3 K0 K1     ; R3["type"] := "call"
    2 [-]: SETTABLE  R3 K2 R0     ; R3["post"] := R0
    3 [-]: NEWTABLE  R4 0 0       ; R4 := {} (size = 0,0)
    4 [-]: VARARG    R5 0         ; R5 to top := ...
    5 [-]: SETLIST   R4 0 1       ; R4[0] to R4[top] := R5 to top ; R(a)[(c-1)*FPF+i] := R(a+i), 1 <= i <= b, a=4, b=0, c=1, FPF=50
    6 [-]: SETTABLE  R3 K3 R4     ; R3["argv"] := R4
    7 [-]: SETTABLE  R3 K4 R1     ; R3["name"] := R1
    8 [-]: GETUPVAL  R4 U0        ; R4 := U0
    9 [-]: SETTABLE  R3 K5 R4     ; R3["target"] := R4
   10 [-]: RETURN    R3 2         ; return R3
   11 [-]: RETURN    R0 1         ; return 


; Function:        0_35
; Defined at line: 1055
; #Upvalues:       0
; #Parameters:     0
; Is_vararg:       3
; Max Stack Size:  4

    0 [-]: GETGLOBAL R1 K0        ; R1 := post_on
    1 [-]: LOADBOOL  R2 1 0       ; R2 := true
    2 [-]: VARARG    R3 0         ; R3 to top := ...
    3 [-]: TAILCALL  R1 0 0       ; R1 to top := R1(R2 to top)
    4 [-]: RETURN    R1 0         ; return R1 to top
    5 [-]: RETURN    R0 1         ; return 


; Function:        0_36
; Defined at line: 1060
; #Upvalues:       0
; #Parameters:     1
; Is_vararg:       7
; Max Stack Size:  4

    0 [-]: GETGLOBAL R2 K0        ; R2 := require
    1 [-]: LOADK     R3 K1        ; R3 := "luci.template"
    2 [-]: CALL      R2 2 2       ; R2 := R2(R3)
    3 [-]: GETTABLE  R2 R2 K2     ; R2 := R2["render"]
    4 [-]: GETTABLE  R3 R0 K3     ; R3 := R0["view"]
    5 [-]: CALL      R2 2 1       ;  := R2(R3)
    6 [-]: RETURN    R0 1         ; return 


; Function:        0_37
; Defined at line: 1064
; #Upvalues:       1
; #Parameters:     1
; Is_vararg:       0
; Max Stack Size:  3

    0 [-]: NEWTABLE  R1 0 3       ; R1 := {} (size = 0,3)
    1 [-]: SETTABLE  R1 K0 K1     ; R1["type"] := "template"
    2 [-]: SETTABLE  R1 K2 R0     ; R1["view"] := R0
    3 [-]: GETUPVAL  R2 U0        ; R2 := U0
    4 [-]: SETTABLE  R1 K3 R2     ; R1["target"] := R2
    5 [-]: RETURN    R1 2         ; return R1
    6 [-]: RETURN    R0 1         ; return 


; Function:        0_38
; Defined at line: 1069
; #Upvalues:       0
; #Parameters:     1
; Is_vararg:       3
; Max Stack Size:  27

    0 [-]: GETGLOBAL R2 K0        ; R2 := require
    1 [-]: LOADK     R3 K1        ; R3 := "luci.cbi"
    2 [-]: CALL      R2 2 2       ; R2 := R2(R3)
    3 [-]: GETGLOBAL R3 K0        ; R3 := require
    4 [-]: LOADK     R4 K2        ; R4 := "luci.template"
    5 [-]: CALL      R3 2 2       ; R3 := R3(R4)
    6 [-]: GETGLOBAL R4 K0        ; R4 := require
    7 [-]: LOADK     R5 K3        ; R5 := "luci.http"
    8 [-]: CALL      R4 2 2       ; R4 := R4(R5)
    9 [-]: GETTABLE  R5 R0 K4     ; R5 := R0["config"]
   10 [-]: TEST      R5 1         ; if not R5 then goto 12 else goto 13
   11 [-]: JMP       1            ; PC += 1 (goto 13)
   12 [-]: NEWTABLE  R5 0 0       ; R5 := {} (size = 0,0)
   13 [-]: GETTABLE  R6 R2 K5     ; R6 := R2["load"]
   14 [-]: GETTABLE  R7 R0 K6     ; R7 := R0["model"]
   15 [-]: VARARG    R8 0         ; R8 to top := ...
   16 [-]: CALL      R6 0 2       ; R6 := R6(R7 to top)
   17 [-]: LOADNIL   R7 R7        ; R7 := nil
   18 [-]: GETGLOBAL R8 K7        ; R8 := ipairs
   19 [-]: MOVE      R9 R6        ; R9 := R6
   20 [-]: CALL      R8 2 4       ; R8 to R10 := R8(R9)
   21 [-]: JMP       10           ; PC += 10 (goto 32)
   22 [-]: SETTABLE  R12 K8 R5    ; R12["flow"] := R5
   23 [-]: SELF      R13 R12 K9   ; R14 := R12; R13 := R12["parse"]
   24 [-]: CALL      R13 2 2      ; R13 := R13(R14)
   25 [-]: TEST      R13 0        ; if R13 then goto 27 else goto 32
   26 [-]: JMP       5            ; PC += 5 (goto 32)
   27 [-]: TEST      R7 0         ; if R7 then goto 29 else goto 31
   28 [-]: JMP       2            ; PC += 2 (goto 31)
   29 [-]: LT        0 R13 R7     ; if R13 < R7 then goto 31 else goto 32
   30 [-]: JMP       1            ; PC += 1 (goto 32)
   31 [-]: MOVE      R7 R13       ; R7 := R13
   32 [-]: TFORLOOP  R8 2         ; R11 to R12 := R8(R9,R10); if R11 ~= nil then R10 := R11 else goto 34
   33 [-]: JMP       -12          ; PC += -12 (goto 22)
   34 [-]: CLOSURE   R8 0         ; R8 := closure(Function #0_38_0)
   35 [-]: GETTABLE  R9 R5 K10    ; R9 := R5["on_valid_to"]
   36 [-]: TEST      R9 0         ; if R9 then goto 38 else goto 50
   37 [-]: JMP       12           ; PC += 12 (goto 50)
   38 [-]: TEST      R7 0         ; if R7 then goto 40 else goto 50
   39 [-]: JMP       10           ; PC += 10 (goto 50)
   40 [-]: LT        0 K11 R7     ; if Unknown_Type_Error < R7 then goto 42 else goto 50
   41 [-]: JMP       8            ; PC += 8 (goto 50)
   42 [-]: LT        0 R7 K12     ; if R7 < Unknown_Type_Error then goto 44 else goto 50
   43 [-]: JMP       6            ; PC += 6 (goto 50)
   44 [-]: GETTABLE  R9 R4 K13    ; R9 := R4["redirect"]
   45 [-]: MOVE      R10 R8       ; R10 := R8
   46 [-]: GETTABLE  R11 R5 K10   ; R11 := R5["on_valid_to"]
   47 [-]: CALL      R10 2 0      ; R10 to top := R10(R11)
   48 [-]: CALL      R9 0 1       ;  := R9(R10 to top)
   49 [-]: RETURN    R0 1         ; return 
   50 [-]: GETTABLE  R9 R5 K14    ; R9 := R5["on_changed_to"]
   51 [-]: TEST      R9 0         ; if R9 then goto 53 else goto 63
   52 [-]: JMP       10           ; PC += 10 (goto 63)
   53 [-]: TEST      R7 0         ; if R7 then goto 55 else goto 63
   54 [-]: JMP       8            ; PC += 8 (goto 63)
   55 [-]: LT        0 K15 R7     ; if Unknown_Type_Error < R7 then goto 57 else goto 63
   56 [-]: JMP       6            ; PC += 6 (goto 63)
   57 [-]: GETTABLE  R9 R4 K13    ; R9 := R4["redirect"]
   58 [-]: MOVE      R10 R8       ; R10 := R8
   59 [-]: GETTABLE  R11 R5 K14   ; R11 := R5["on_changed_to"]
   60 [-]: CALL      R10 2 0      ; R10 to top := R10(R11)
   61 [-]: CALL      R9 0 1       ;  := R9(R10 to top)
   62 [-]: RETURN    R0 1         ; return 
   63 [-]: GETTABLE  R9 R5 K16    ; R9 := R5["on_success_to"]
   64 [-]: TEST      R9 0         ; if R9 then goto 66 else goto 76
   65 [-]: JMP       10           ; PC += 10 (goto 76)
   66 [-]: TEST      R7 0         ; if R7 then goto 68 else goto 76
   67 [-]: JMP       8            ; PC += 8 (goto 76)
   68 [-]: LT        0 K11 R7     ; if Unknown_Type_Error < R7 then goto 70 else goto 76
   69 [-]: JMP       6            ; PC += 6 (goto 76)
   70 [-]: GETTABLE  R9 R4 K13    ; R9 := R4["redirect"]
   71 [-]: MOVE      R10 R8       ; R10 := R8
   72 [-]: GETTABLE  R11 R5 K16   ; R11 := R5["on_success_to"]
   73 [-]: CALL      R10 2 0      ; R10 to top := R10(R11)
   74 [-]: CALL      R9 0 1       ;  := R9(R10 to top)
   75 [-]: RETURN    R0 1         ; return 
   76 [-]: GETTABLE  R9 R5 K17    ; R9 := R5["state_handler"]
   77 [-]: TEST      R9 0         ; if R9 then goto 79 else goto 86
   78 [-]: JMP       7            ; PC += 7 (goto 86)
   79 [-]: GETTABLE  R9 R5 K17    ; R9 := R5["state_handler"]
   80 [-]: MOVE      R10 R7       ; R10 := R7
   81 [-]: MOVE      R11 R6       ; R11 := R6
   82 [-]: CALL      R9 3 2       ; R9 := R9(R10 to R11)
   83 [-]: TEST      R9 1         ; if not R9 then goto 85 else goto 86
   84 [-]: JMP       1            ; PC += 1 (goto 86)
   85 [-]: RETURN    R0 1         ; return 
   86 [-]: GETTABLE  R9 R4 K18    ; R9 := R4["header"]
   87 [-]: LOADK     R10 K19      ; R10 := "X-CBI-State"
   88 [-]: TESTSET   R11 R7 1     ; if R7 then R11 := R7 ; goto 91 else goto 90
   89 [-]: JMP       1            ; PC += 1 (goto 91)
   90 [-]: LOADK     R11 K11      ; R11 := Unknown_Type_Error
   91 [-]: CALL      R9 3 1       ;  := R9(R10 to R11)
   92 [-]: LOADNIL   R9 R11       ; R9 to R11 := nil
   93 [-]: LOADBOOL  R12 0 0      ; R12 := false
   94 [-]: LOADBOOL  R13 1 0      ; R13 := true
   95 [-]: NEWTABLE  R14 0 0      ; R14 := {} (size = 0,0)
   96 [-]: GETGLOBAL R15 K7       ; R15 := ipairs
   97 [-]: MOVE      R16 R6       ; R16 := R6
   98 [-]: CALL      R15 2 4      ; R15 to R17 := R15(R16)
   99 [-]: JMP       44           ; PC += 44 (goto 144)
  100 [-]: GETTABLE  R20 R19 K20  ; R20 := R19["apply_needed"]
  101 [-]: TEST      R20 0        ; if R20 then goto 103 else goto 117
  102 [-]: JMP       14           ; PC += 14 (goto 117)
  103 [-]: GETTABLE  R20 R19 K21  ; R20 := R19["parsechain"]
  104 [-]: TEST      R20 0        ; if R20 then goto 106 else goto 116
  105 [-]: JMP       10           ; PC += 10 (goto 116)
  106 [-]: LOADNIL   R20 R20      ; R20 := nil
  107 [-]: GETGLOBAL R21 K7       ; R21 := ipairs
  108 [-]: GETTABLE  R22 R19 K21  ; R22 := R19["parsechain"]
  109 [-]: CALL      R21 2 4      ; R21 to R23 := R21(R22)
  110 [-]: JMP       3            ; PC += 3 (goto 114)
  111 [-]: LEN       R26 R14      ; R26 := #R14
  112 [-]: ADD       R26 R26 K15  ; R26 := R26 + Unknown_Type_Error
  113 [-]: SETTABLE  R14 R26 R25  ; R14[R26] := R25
  114 [-]: TFORLOOP  R21 2        ; R24 to R25 := R21(R22,R23); if R24 ~= nil then R23 := R24 else goto 116
  115 [-]: JMP       -5           ; PC += -5 (goto 111)
  116 [-]: LOADBOOL  R12 1 0      ; R12 := true
  117 [-]: GETTABLE  R20 R19 K22  ; R20 := R19["nextbtn"]
  118 [-]: TEST      R20 0        ; if R20 then goto 120 else goto 123
  119 [-]: JMP       3            ; PC += 3 (goto 123)
  120 [-]: TEST      R9 1         ; if not R9 then goto 122 else goto 123
  121 [-]: JMP       1            ; PC += 1 (goto 123)
  122 [-]: GETTABLE  R9 R19 K22   ; R9 := R19["nextbtn"]
  123 [-]: GETTABLE  R20 R19 K13  ; R20 := R19["redirect"]
  124 [-]: TEST      R20 0        ; if R20 then goto 126 else goto 129
  125 [-]: JMP       3            ; PC += 3 (goto 129)
  126 [-]: TEST      R10 1        ; if not R10 then goto 128 else goto 129
  127 [-]: JMP       1            ; PC += 1 (goto 129)
  128 [-]: GETTABLE  R10 R19 K13  ; R10 := R19["redirect"]
  129 [-]: GETTABLE  R20 R19 K23  ; R20 := R19["pageaction"]
  130 [-]: EQ        0 R20 K24    ; if R20 == false then goto 132 else goto 133
  131 [-]: JMP       1            ; PC += 1 (goto 133)
  132 [-]: LOADBOOL  R13 0 0      ; R13 := false
  133 [-]: GETTABLE  R20 R19 K25  ; R20 := R19["message"]
  134 [-]: TEST      R20 0        ; if R20 then goto 136 else goto 144
  135 [-]: JMP       8            ; PC += 8 (goto 144)
  136 [-]: TEST      R11 1        ; if not R11 then goto 138 else goto 140
  137 [-]: JMP       2            ; PC += 2 (goto 140)
  138 [-]: NEWTABLE  R20 0 0      ; R20 := {} (size = 0,0)
  139 [-]: MOVE      R11 R20      ; R11 := R20
  140 [-]: LEN       R20 R11      ; R20 := #R11
  141 [-]: ADD       R20 R20 K15  ; R20 := R20 + Unknown_Type_Error
  142 [-]: GETTABLE  R21 R19 K25  ; R21 := R19["message"]
  143 [-]: SETTABLE  R11 R20 R21  ; R11[R20] := R21
  144 [-]: TFORLOOP  R15 2        ; R18 to R19 := R15(R16,R17); if R18 ~= nil then R17 := R18 else goto 146
  145 [-]: JMP       -46          ; PC += -46 (goto 100)
  146 [-]: TEST      R12 0        ; if R12 then goto 148 else goto 166
  147 [-]: JMP       18           ; PC += 18 (goto 166)
  148 [-]: TEST      R10 0        ; if R10 then goto 150 else goto 159
  149 [-]: JMP       9            ; PC += 9 (goto 159)
  150 [-]: LEN       R15 R14      ; R15 := #R14
  151 [-]: EQ        0 R15 K11    ; if R15 == Unknown_Type_Error then goto 153 else goto 159
  152 [-]: JMP       6            ; PC += 6 (goto 159)
  153 [-]: GETTABLE  R15 R4 K13   ; R15 := R4["redirect"]
  154 [-]: MOVE      R16 R8       ; R16 := R8
  155 [-]: MOVE      R17 R10      ; R17 := R10
  156 [-]: CALL      R16 2 0      ; R16 to top := R16(R17)
  157 [-]: CALL      R15 0 1      ;  := R15(R16 to top)
  158 [-]: JMP       46           ; PC += 46 (goto 205)
  159 [-]: GETTABLE  R15 R3 K26   ; R15 := R3["render"]
  160 [-]: LOADK     R16 K27      ; R16 := "cbi/apply_xhr"
  161 [-]: NEWTABLE  R17 0 2      ; R17 := {} (size = 0,2)
  162 [-]: SETTABLE  R17 K13 R10  ; R17["redirect"] := R10
  163 [-]: SETTABLE  R17 K21 R14  ; R17["parsechain"] := R14
  164 [-]: CALL      R15 3 1      ;  := R15(R16 to R17)
  165 [-]: JMP       39           ; PC += 39 (goto 205)
  166 [-]: GETTABLE  R15 R5 K28   ; R15 := R5["noheader"]
  167 [-]: TEST      R15 1        ; if not R15 then goto 169 else goto 174
  168 [-]: JMP       5            ; PC += 5 (goto 174)
  169 [-]: GETTABLE  R15 R3 K26   ; R15 := R3["render"]
  170 [-]: LOADK     R16 K29      ; R16 := "cbi/header"
  171 [-]: NEWTABLE  R17 0 1      ; R17 := {} (size = 0,1)
  172 [-]: SETTABLE  R17 K30 R7   ; R17["state"] := R7
  173 [-]: CALL      R15 3 1      ;  := R15(R16 to R17)
  174 [-]: GETGLOBAL R15 K7       ; R15 := ipairs
  175 [-]: MOVE      R16 R6       ; R16 := R6
  176 [-]: CALL      R15 2 4      ; R15 to R17 := R15(R16)
  177 [-]: JMP       14           ; PC += 14 (goto 192)
  178 [-]: SELF      R20 R19 K26  ; R21 := R19; R20 := R19["render"]
  179 [-]: NEWTABLE  R22 0 6      ; R22 := {} (size = 0,6)
  180 [-]: EQ        1 R18 K15    ; if R18 ~= Unknown_Type_Error then goto 182 else goto 183
  181 [-]: JMP       1            ; PC += 1 (goto 183)
  182 [-]: LOADBOOL  R23 0 1      ; R23 := false; goto 184
  183 [-]: LOADBOOL  R23 1 0      ; R23 := true
  184 [-]: SETTABLE  R22 K31 R23  ; R22["firstmap"] := R23
  185 [-]: LEN       R23 R6       ; R23 := #R6
  186 [-]: SETTABLE  R22 K32 R23  ; R22["maps"] := R23
  187 [-]: SETTABLE  R22 K13 R10  ; R22["redirect"] := R10
  188 [-]: SETTABLE  R22 K33 R11  ; R22["messages"] := R11
  189 [-]: SETTABLE  R22 K23 R13  ; R22["pageaction"] := R13
  190 [-]: SETTABLE  R22 K21 R14  ; R22["parsechain"] := R14
  191 [-]: CALL      R20 3 1      ;  := R20(R21 to R22)
  192 [-]: TFORLOOP  R15 2        ; R18 to R19 := R15(R16,R17); if R18 ~= nil then R17 := R18 else goto 194
  193 [-]: JMP       -16          ; PC += -16 (goto 178)
  194 [-]: GETTABLE  R15 R5 K34   ; R15 := R5["nofooter"]
  195 [-]: TEST      R15 1        ; if not R15 then goto 197 else goto 205
  196 [-]: JMP       8            ; PC += 8 (goto 205)
  197 [-]: GETTABLE  R15 R3 K26   ; R15 := R3["render"]
  198 [-]: LOADK     R16 K35      ; R16 := "cbi/footer"
  199 [-]: NEWTABLE  R17 0 4      ; R17 := {} (size = 0,4)
  200 [-]: SETTABLE  R17 K8 R5    ; R17["flow"] := R5
  201 [-]: SETTABLE  R17 K23 R13  ; R17["pageaction"] := R13
  202 [-]: SETTABLE  R17 K30 R7   ; R17["state"] := R7
  203 [-]: SETTABLE  R17 K22 R9   ; R17["nextbtn"] := R9
  204 [-]: CALL      R15 3 1      ;  := R15(R16 to R17)
  205 [-]: RETURN    R0 1         ; return 


; Function:        0_38_0
; Defined at line: 1087
; #Upvalues:       0
; #Parameters:     1
; Is_vararg:       0
; Max Stack Size:  4

    0 [-]: GETGLOBAL R1 K0        ; R1 := type
    1 [-]: MOVE      R2 R0        ; R2 := R0
    2 [-]: CALL      R1 2 2       ; R1 := R1(R2)
    3 [-]: EQ        0 R1 K1      ; if R1 == "table" then goto 5 else goto 12
    4 [-]: JMP       7            ; PC += 7 (goto 12)
    5 [-]: GETGLOBAL R1 K2        ; R1 := build_url
    6 [-]: GETGLOBAL R2 K3        ; R2 := unpack
    7 [-]: MOVE      R3 R0        ; R3 := R0
    8 [-]: CALL      R2 2 0       ; R2 to top := R2(R3)
    9 [-]: CALL      R1 0 2       ; R1 := R1(R2 to top)
   10 [-]: TEST      R1 1         ; if not R1 then goto 12 else goto 13
   11 [-]: JMP       1            ; PC += 1 (goto 13)
   12 [-]: MOVE      R1 R0        ; R1 := R0
   13 [-]: RETURN    R1 2         ; return R1
   14 [-]: RETURN    R0 1         ; return 


; Function:        0_39
; Defined at line: 1186
; #Upvalues:       1
; #Parameters:     2
; Is_vararg:       0
; Max Stack Size:  4

    0 [-]: NEWTABLE  R2 0 5       ; R2 := {} (size = 0,5)
    1 [-]: SETTABLE  R2 K0 K1     ; R2["type"] := "cbi"
    2 [-]: NEWTABLE  R3 0 1       ; R3 := {} (size = 0,1)
    3 [-]: SETTABLE  R3 K3 K4     ; R3["cbi.submit"] := "1"
    4 [-]: SETTABLE  R2 K2 R3     ; R2["post"] := R3
    5 [-]: SETTABLE  R2 K5 R1     ; R2["config"] := R1
    6 [-]: SETTABLE  R2 K6 R0     ; R2["model"] := R0
    7 [-]: GETUPVAL  R3 U0        ; R3 := U0
    8 [-]: SETTABLE  R2 K7 R3     ; R2["target"] := R3
    9 [-]: RETURN    R2 2         ; return R2
   10 [-]: RETURN    R0 1         ; return 


; Function:        0_40
; Defined at line: 1197
; #Upvalues:       0
; #Parameters:     1
; Is_vararg:       3
; Max Stack Size:  8

    0 [-]: NEWTABLE  R2 0 0       ; R2 := {} (size = 0,0)
    1 [-]: VARARG    R3 0         ; R3 to top := ...
    2 [-]: SETLIST   R2 0 1       ; R2[0] to R2[top] := R3 to top ; R(a)[(c-1)*FPF+i] := R(a+i), 1 <= i <= b, a=2, b=0, c=1, FPF=50
    3 [-]: LEN       R3 R2        ; R3 := #R2
    4 [-]: LT        0 K0 R3      ; if Unknown_Type_Error < R3 then goto 6 else goto 10
    5 [-]: JMP       4            ; PC += 4 (goto 10)
    6 [-]: GETTABLE  R3 R0 K1     ; R3 := R0["targets"]
    7 [-]: GETTABLE  R3 R3 K2     ; R3 := R3[Unknown_Type_Error]
    8 [-]: TEST      R3 1         ; if not R3 then goto 10 else goto 12
    9 [-]: JMP       2            ; PC += 2 (goto 12)
   10 [-]: GETTABLE  R3 R0 K1     ; R3 := R0["targets"]
   11 [-]: GETTABLE  R3 R3 K3     ; R3 := R3[Unknown_Type_Error]
   12 [-]: GETGLOBAL R4 K4        ; R4 := setfenv
   13 [-]: GETTABLE  R5 R3 K5     ; R5 := R3["target"]
   14 [-]: GETTABLE  R6 R0 K6     ; R6 := R0["env"]
   15 [-]: CALL      R4 3 1       ;  := R4(R5 to R6)
   16 [-]: SELF      R4 R3 K5     ; R5 := R3; R4 := R3["target"]
   17 [-]: GETGLOBAL R6 K7        ; R6 := unpack
   18 [-]: MOVE      R7 R2        ; R7 := R2
   19 [-]: CALL      R6 2 0       ; R6 to top := R6(R7)
   20 [-]: CALL      R4 0 1       ;  := R4(R5 to top)
   21 [-]: RETURN    R0 1         ; return 


; Function:        0_41
; Defined at line: 1204
; #Upvalues:       1
; #Parameters:     2
; Is_vararg:       0
; Max Stack Size:  6

    0 [-]: NEWTABLE  R2 0 4       ; R2 := {} (size = 0,4)
    1 [-]: SETTABLE  R2 K0 K1     ; R2["type"] := "arcombine"
    2 [-]: GETGLOBAL R3 K3        ; R3 := getfenv
    3 [-]: CALL      R3 1 2       ; R3 := R3()
    4 [-]: SETTABLE  R2 K2 R3     ; R2["env"] := R3
    5 [-]: GETUPVAL  R3 U0        ; R3 := U0
    6 [-]: SETTABLE  R2 K4 R3     ; R2["target"] := R3
    7 [-]: NEWTABLE  R3 2 0       ; R3 := {} (size = 2,0)
    8 [-]: MOVE      R4 R0        ; R4 := R0
    9 [-]: MOVE      R5 R1        ; R5 := R1
   10 [-]: SETLIST   R3 2 1       ; R3[0] to R3[1] := R4 to R5 ; R(a)[(c-1)*FPF+i] := R(a+i), 1 <= i <= b, a=3, b=2, c=1, FPF=50
   11 [-]: SETTABLE  R2 K5 R3     ; R2["targets"] := R3
   12 [-]: RETURN    R2 2         ; return R2
   13 [-]: RETURN    R0 1         ; return 


; Function:        0_42
; Defined at line: 1209
; #Upvalues:       0
; #Parameters:     1
; Is_vararg:       3
; Max Stack Size:  14

    0 [-]: GETGLOBAL R2 K0        ; R2 := require
    1 [-]: LOADK     R3 K1        ; R3 := "luci.cbi"
    2 [-]: CALL      R2 2 2       ; R2 := R2(R3)
    3 [-]: GETGLOBAL R3 K0        ; R3 := require
    4 [-]: LOADK     R4 K2        ; R4 := "luci.template"
    5 [-]: CALL      R3 2 2       ; R3 := R3(R4)
    6 [-]: GETGLOBAL R4 K0        ; R4 := require
    7 [-]: LOADK     R5 K3        ; R5 := "luci.http"
    8 [-]: CALL      R4 2 2       ; R4 := R4(R5)
    9 [-]: GETGLOBAL R5 K4        ; R5 := luci
   10 [-]: GETTABLE  R5 R5 K5     ; R5 := R5["cbi"]
   11 [-]: GETTABLE  R5 R5 K6     ; R5 := R5["load"]
   12 [-]: GETTABLE  R6 R0 K7     ; R6 := R0["model"]
   13 [-]: VARARG    R7 0         ; R7 to top := ...
   14 [-]: CALL      R5 0 2       ; R5 := R5(R6 to top)
   15 [-]: LOADNIL   R6 R6        ; R6 := nil
   16 [-]: GETGLOBAL R7 K8        ; R7 := ipairs
   17 [-]: MOVE      R8 R5        ; R8 := R5
   18 [-]: CALL      R7 2 4       ; R7 to R9 := R7(R8)
   19 [-]: JMP       9            ; PC += 9 (goto 29)
   20 [-]: SELF      R12 R11 K9   ; R13 := R11; R12 := R11["parse"]
   21 [-]: CALL      R12 2 2      ; R12 := R12(R13)
   22 [-]: TEST      R12 0        ; if R12 then goto 24 else goto 29
   23 [-]: JMP       5            ; PC += 5 (goto 29)
   24 [-]: TEST      R6 0         ; if R6 then goto 26 else goto 28
   25 [-]: JMP       2            ; PC += 2 (goto 28)
   26 [-]: LT        0 R12 R6     ; if R12 < R6 then goto 28 else goto 29
   27 [-]: JMP       1            ; PC += 1 (goto 29)
   28 [-]: MOVE      R6 R12       ; R6 := R12
   29 [-]: TFORLOOP  R7 2         ; R10 to R11 := R7(R8,R9); if R10 ~= nil then R9 := R10 else goto 31
   30 [-]: JMP       -11          ; PC += -11 (goto 20)
   31 [-]: GETTABLE  R7 R4 K10    ; R7 := R4["header"]
   32 [-]: LOADK     R8 K11       ; R8 := "X-CBI-State"
   33 [-]: TESTSET   R9 R6 1      ; if R6 then R9 := R6 ; goto 36 else goto 35
   34 [-]: JMP       1            ; PC += 1 (goto 36)
   35 [-]: LOADK     R9 K12       ; R9 := Unknown_Type_Error
   36 [-]: CALL      R7 3 1       ;  := R7(R8 to R9)
   37 [-]: GETTABLE  R7 R3 K13    ; R7 := R3["render"]
   38 [-]: LOADK     R8 K10       ; R8 := "header"
   39 [-]: CALL      R7 2 1       ;  := R7(R8)
   40 [-]: GETGLOBAL R7 K8        ; R7 := ipairs
   41 [-]: MOVE      R8 R5        ; R8 := R5
   42 [-]: CALL      R7 2 4       ; R7 to R9 := R7(R8)
   43 [-]: JMP       2            ; PC += 2 (goto 46)
   44 [-]: SELF      R12 R11 K13  ; R13 := R11; R12 := R11["render"]
   45 [-]: CALL      R12 2 1      ;  := R12(R13)
   46 [-]: TFORLOOP  R7 2         ; R10 to R11 := R7(R8,R9); if R10 ~= nil then R9 := R10 else goto 48
   47 [-]: JMP       -4           ; PC += -4 (goto 44)
   48 [-]: GETTABLE  R7 R3 K13    ; R7 := R3["render"]
   49 [-]: LOADK     R8 K14       ; R8 := "footer"
   50 [-]: CALL      R7 2 1       ;  := R7(R8)
   51 [-]: RETURN    R0 1         ; return 


; Function:        0_43
; Defined at line: 1232
; #Upvalues:       1
; #Parameters:     1
; Is_vararg:       0
; Max Stack Size:  3

    0 [-]: NEWTABLE  R1 0 4       ; R1 := {} (size = 0,4)
    1 [-]: SETTABLE  R1 K0 K1     ; R1["type"] := "cbi"
    2 [-]: NEWTABLE  R2 0 1       ; R2 := {} (size = 0,1)
    3 [-]: SETTABLE  R2 K3 K4     ; R2["cbi.submit"] := "1"
    4 [-]: SETTABLE  R1 K2 R2     ; R1["post"] := R2
    5 [-]: SETTABLE  R1 K5 R0     ; R1["model"] := R0
    6 [-]: GETUPVAL  R2 U0        ; R2 := U0
    7 [-]: SETTABLE  R1 K6 R2     ; R1["target"] := R2
    8 [-]: RETURN    R1 2         ; return R1
    9 [-]: RETURN    R0 1         ; return 


; Function:        0_44
; Defined at line: 1245
; #Upvalues:       0
; #Parameters:     1
; Is_vararg:       0
; Max Stack Size:  2

    0 [-]: RETURN    R0 2         ; return R0
    1 [-]: RETURN    R0 1         ; return 


