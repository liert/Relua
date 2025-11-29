; Function:        0
; Defined at line: 0
; #Upvalues:       0
; #Parameters:     0
; Is_vararg:       2
; Max Stack Size:  18

    0 [-]: GETGLOBAL R0 K0        ; R0 := require
    1 [-]: LOADK     R1 K1        ; R1 := "luci.util"
    2 [-]: CALL      R0 2 2       ; R0 := R0(R1)
    3 [-]: GETGLOBAL R1 K0        ; R1 := require
    4 [-]: LOADK     R2 K2        ; R2 := "luci.config"
    5 [-]: CALL      R1 2 2       ; R1 := R1(R2)
    6 [-]: GETGLOBAL R2 K0        ; R2 := require
    7 [-]: LOADK     R3 K3        ; R3 := "luci.template.parser"
    8 [-]: CALL      R2 2 2       ; R2 := R2(R3)
    9 [-]: GETGLOBAL R3 K4        ; R3 := tostring
   10 [-]: GETGLOBAL R4 K5        ; R4 := pairs
   11 [-]: GETGLOBAL R5 K6        ; R5 := loadstring
   12 [-]: GETGLOBAL R6 K7        ; R6 := setmetatable
   13 [-]: GETGLOBAL R7 K8        ; R7 := loadfile
   14 [-]: GETGLOBAL R8 K9        ; R8 := getfenv
   15 [-]: GETGLOBAL R9 K10       ; R9 := setfenv
   16 [-]: GETGLOBAL R10 K11      ; R10 := rawget
   17 [-]: GETGLOBAL R11 K12      ; R11 := assert
   18 [-]: GETGLOBAL R12 K13      ; R12 := type
   19 [-]: GETGLOBAL R13 K14      ; R13 := error
   20 [-]: GETGLOBAL R14 K15      ; R14 := module
   21 [-]: LOADK     R15 K16      ; R15 := "luci.template"
   22 [-]: CALL      R14 2 1      ;  := R14(R15)
   23 [-]: GETTABLE  R14 R1 K17   ; R14 := R1["template"]
   24 [-]: TEST      R14 1        ; if not R14 then goto 26 else goto 27
   25 [-]: JMP       1            ; PC += 1 (goto 27)
   26 [-]: NEWTABLE  R14 0 0      ; R14 := {} (size = 0,0)
   27 [-]: SETTABLE  R1 K17 R14   ; R1["template"] := R14
   28 [-]: GETTABLE  R14 R1 K17   ; R14 := R1["template"]
   29 [-]: GETTABLE  R14 R14 K18  ; R14 := R14["viewdir"]
   30 [-]: TEST      R14 1        ; if not R14 then goto 32 else goto 36
   31 [-]: JMP       4            ; PC += 4 (goto 36)
   32 [-]: GETTABLE  R14 R0 K19   ; R14 := R0["libpath"]
   33 [-]: CALL      R14 1 2      ; R14 := R14()
   34 [-]: LOADK     R15 K20      ; R15 := "/view"
   35 [-]: CONCAT    R14 R14 R15  ; R14 := concat(R14 to R15)
   36 [-]: SETGLOBAL R14 K18      ; viewdir := R14
   37 [-]: GETTABLE  R14 R0 K22   ; R14 := R0["threadlocal"]
   38 [-]: CALL      R14 1 2      ; R14 := R14()
   39 [-]: SETGLOBAL R14 K21      ; context := R14
   40 [-]: CLOSURE   R14 0        ; R14 := closure(Function #0_0)
   41 [-]: MOVE      R0 R8        ; R0 := R8
   42 [-]: SETGLOBAL R14 K23      ; render := R14
   43 [-]: CLOSURE   R14 1        ; R14 := closure(Function #0_1)
   44 [-]: MOVE      R0 R8        ; R0 := R8
   45 [-]: SETGLOBAL R14 K24      ; render_string := R14
   46 [-]: GETTABLE  R14 R0 K26   ; R14 := R0["class"]
   47 [-]: CALL      R14 1 2      ; R14 := R14()
   48 [-]: SETGLOBAL R14 K25      ; Template := R14
   49 [-]: GETGLOBAL R14 K25      ; R14 := Template
   50 [-]: MOVE      R15 R6       ; R15 := R6
   51 [-]: NEWTABLE  R16 0 0      ; R16 := {} (size = 0,0)
   52 [-]: NEWTABLE  R17 0 1      ; R17 := {} (size = 0,1)
   53 [-]: SETTABLE  R17 K28 K29  ; R17["__mode"] := "v"
   54 [-]: CALL      R15 3 2      ; R15 := R15(R16 to R17)
   55 [-]: SETTABLE  R14 K27 R15  ; R14["cache"] := R15
   56 [-]: GETGLOBAL R14 K25      ; R14 := Template
   57 [-]: CLOSURE   R15 2        ; R15 := closure(Function #0_2)
   58 [-]: MOVE      R0 R2        ; R0 := R2
   59 [-]: MOVE      R0 R13       ; R0 := R13
   60 [-]: SETTABLE  R14 K30 R15  ; R14["__init__"] := R15
   61 [-]: GETGLOBAL R14 K25      ; R14 := Template
   62 [-]: CLOSURE   R15 3        ; R15 := closure(Function #0_3)
   63 [-]: MOVE      R0 R8        ; R0 := R8
   64 [-]: MOVE      R0 R9        ; R0 := R9
   65 [-]: MOVE      R0 R6        ; R0 := R6
   66 [-]: MOVE      R0 R10       ; R0 := R10
   67 [-]: MOVE      R0 R0        ; R0 := R0
   68 [-]: MOVE      R0 R13       ; R0 := R13
   69 [-]: MOVE      R0 R3        ; R0 := R3
   70 [-]: SETTABLE  R14 K23 R15  ; R14["render"] := R15
   71 [-]: RETURN    R0 1         ; return 


; Function:        0_0
; Defined at line: 26
; #Upvalues:       1
; #Parameters:     2
; Is_vararg:       0
; Max Stack Size:  6

    0 [-]: GETGLOBAL R2 K0        ; R2 := Template
    1 [-]: MOVE      R3 R0        ; R3 := R0
    2 [-]: CALL      R2 2 2       ; R2 := R2(R3)
    3 [-]: SELF      R2 R2 K1     ; R3 := R2; R2 := R2["render"]
    4 [-]: TESTSET   R4 R1 1      ; if R1 then R4 := R1 ; goto 9 else goto 6
    5 [-]: JMP       3            ; PC += 3 (goto 9)
    6 [-]: GETUPVAL  R4 U0        ; R4 := U0
    7 [-]: LOADK     R5 K2        ; R5 := Unknown_Type_Error
    8 [-]: CALL      R4 2 2       ; R4 := R4(R5)
    9 [-]: TAILCALL  R2 3 0       ; R2 to top := R2(R3 to R4)
   10 [-]: RETURN    R2 0         ; return R2 to top
   11 [-]: RETURN    R0 1         ; return 


; Function:        0_1
; Defined at line: 33
; #Upvalues:       1
; #Parameters:     2
; Is_vararg:       0
; Max Stack Size:  6

    0 [-]: GETGLOBAL R2 K0        ; R2 := Template
    1 [-]: LOADNIL   R3 R3        ; R3 := nil
    2 [-]: MOVE      R4 R0        ; R4 := R0
    3 [-]: CALL      R2 3 2       ; R2 := R2(R3 to R4)
    4 [-]: SELF      R2 R2 K1     ; R3 := R2; R2 := R2["render"]
    5 [-]: TESTSET   R4 R1 1      ; if R1 then R4 := R1 ; goto 10 else goto 7
    6 [-]: JMP       3            ; PC += 3 (goto 10)
    7 [-]: GETUPVAL  R4 U0        ; R4 := U0
    8 [-]: LOADK     R5 K2        ; R5 := Unknown_Type_Error
    9 [-]: CALL      R4 2 2       ; R4 := R4(R5)
   10 [-]: TAILCALL  R2 3 0       ; R2 to top := R2(R3 to R4)
   11 [-]: RETURN    R2 0         ; return R2 to top
   12 [-]: RETURN    R0 1         ; return 


; Function:        0_2
; Defined at line: 46
; #Upvalues:       2
; #Parameters:     3
; Is_vararg:       0
; Max Stack Size:  13

    0 [-]: TEST      R1 0         ; if R1 then goto 2 else goto 7
    1 [-]: JMP       5            ; PC += 5 (goto 7)
    2 [-]: GETTABLE  R3 R0 K1     ; R3 := R0["cache"]
    3 [-]: GETTABLE  R3 R3 R1     ; R3 := R3[R1]
    4 [-]: SETTABLE  R0 K0 R3     ; R0["template"] := R3
    5 [-]: SETTABLE  R0 K2 R1     ; R0["name"] := R1
    6 [-]: JMP       1            ; PC += 1 (goto 8)
    7 [-]: SETTABLE  R0 K2 K3     ; R0["name"] := "[string]"
    8 [-]: GETGLOBAL R3 K5        ; R3 := context
    9 [-]: GETTABLE  R3 R3 K4     ; R3 := R3["viewns"]
   10 [-]: SETTABLE  R0 K4 R3     ; R0["viewns"] := R3
   11 [-]: GETTABLE  R3 R0 K0     ; R3 := R0["template"]
   12 [-]: TEST      R3 1         ; if not R3 then goto 14 else goto 59
   13 [-]: JMP       45           ; PC += 45 (goto 59)
   14 [-]: LOADNIL   R3 R4        ; R3 to R4 := nil
   15 [-]: TEST      R1 0         ; if R1 then goto 17 else goto 30
   16 [-]: JMP       13           ; PC += 13 (goto 30)
   17 [-]: GETGLOBAL R5 K6        ; R5 := viewdir
   18 [-]: LOADK     R6 K7        ; R6 := "/"
   19 [-]: MOVE      R7 R1        ; R7 := R1
   20 [-]: LOADK     R8 K8        ; R8 := ".htm"
   21 [-]: CONCAT    R4 R5 R8     ; R4 := concat(R5 to R8)
   22 [-]: GETUPVAL  R5 U0        ; R5 := U0
   23 [-]: GETTABLE  R5 R5 K10    ; R5 := R5["parse"]
   24 [-]: MOVE      R6 R4        ; R6 := R4
   25 [-]: CALL      R5 2 4       ; R5 to R7 := R5(R6)
   26 [-]: MOVE      R3 R7        ; R3 := R7
   27 [-]: SETGLOBAL R6 K9        ; _ := R6
   28 [-]: SETTABLE  R0 K0 R5     ; R0["template"] := R5
   29 [-]: JMP       8            ; PC += 8 (goto 38)
   30 [-]: LOADK     R4 K3        ; R4 := "[string]"
   31 [-]: GETUPVAL  R5 U0        ; R5 := U0
   32 [-]: GETTABLE  R5 R5 K11    ; R5 := R5["parse_string"]
   33 [-]: MOVE      R6 R2        ; R6 := R2
   34 [-]: CALL      R5 2 4       ; R5 to R7 := R5(R6)
   35 [-]: MOVE      R3 R7        ; R3 := R7
   36 [-]: SETGLOBAL R6 K9        ; _ := R6
   37 [-]: SETTABLE  R0 K0 R5     ; R0["template"] := R5
   38 [-]: GETTABLE  R5 R0 K0     ; R5 := R0["template"]
   39 [-]: TEST      R5 1         ; if not R5 then goto 41 else goto 54
   40 [-]: JMP       13           ; PC += 13 (goto 54)
   41 [-]: GETUPVAL  R5 U1        ; R5 := U1
   42 [-]: LOADK     R6 K12       ; R6 := "Failed to load template \'"
   43 [-]: MOVE      R7 R1        ; R7 := R1
   44 [-]: LOADK     R8 K13       ; R8 := "\'.\n"
   45 [-]: LOADK     R9 K14       ; R9 := "Error while parsing template \'"
   46 [-]: MOVE      R10 R4       ; R10 := R4
   47 [-]: LOADK     R11 K15      ; R11 := "\':\n"
   48 [-]: TESTSET   R12 R3 1     ; if R3 then R12 := R3 ; goto 51 else goto 50
   49 [-]: JMP       1            ; PC += 1 (goto 51)
   50 [-]: LOADK     R12 K16      ; R12 := "Unknown syntax error"
   51 [-]: CONCAT    R6 R6 R12    ; R6 := concat(R6 to R12)
   52 [-]: CALL      R5 2 1       ;  := R5(R6)
   53 [-]: JMP       5            ; PC += 5 (goto 59)
   54 [-]: TEST      R1 0         ; if R1 then goto 56 else goto 59
   55 [-]: JMP       3            ; PC += 3 (goto 59)
   56 [-]: GETTABLE  R5 R0 K1     ; R5 := R0["cache"]
   57 [-]: GETTABLE  R6 R0 K0     ; R6 := R0["template"]
   58 [-]: SETTABLE  R5 R1 R6     ; R5[R1] := R6
   59 [-]: RETURN    R0 1         ; return 


; Function:        0_3
; Defined at line: 85
; #Upvalues:       7
; #Parameters:     2
; Is_vararg:       0
; Max Stack Size:  11

    0 [-]: TEST      R1 1         ; if not R1 then goto 2 else goto 6
    1 [-]: JMP       4            ; PC += 4 (goto 6)
    2 [-]: GETUPVAL  R2 U0        ; R2 := U0
    3 [-]: LOADK     R3 K0        ; R3 := Unknown_Type_Error
    4 [-]: CALL      R2 2 2       ; R2 := R2(R3)
    5 [-]: MOVE      R1 R2        ; R1 := R2
    6 [-]: GETUPVAL  R2 U1        ; R2 := U1
    7 [-]: GETTABLE  R3 R0 K1     ; R3 := R0["template"]
    8 [-]: GETUPVAL  R4 U2        ; R4 := U2
    9 [-]: NEWTABLE  R5 0 0       ; R5 := {} (size = 0,0)
   10 [-]: NEWTABLE  R6 0 1       ; R6 := {} (size = 0,1)
   11 [-]: CLOSURE   R7 0         ; R7 := closure(Function #0_3_0)
   12 [-]: GETUPVAL  R0 U3        ; R0 := U3
   13 [-]: MOVE      R0 R0        ; R0 := R0
   14 [-]: MOVE      R0 R1        ; R0 := R1
   15 [-]: SETTABLE  R6 K2 R7     ; R6["__index"] := R7
   16 [-]: CALL      R4 3 0       ; R4 to top := R4(R5 to R6)
   17 [-]: CALL      R2 0 1       ;  := R2(R3 to top)
   18 [-]: GETUPVAL  R2 U4        ; R2 := U4
   19 [-]: GETTABLE  R2 R2 K3     ; R2 := R2["copcall"]
   20 [-]: GETTABLE  R3 R0 K1     ; R3 := R0["template"]
   21 [-]: CALL      R2 2 3       ; R2 to R3 := R2(R3)
   22 [-]: TEST      R2 1         ; if not R2 then goto 24 else goto 36
   23 [-]: JMP       12           ; PC += 12 (goto 36)
   24 [-]: GETUPVAL  R4 U5        ; R4 := U5
   25 [-]: LOADK     R5 K4        ; R5 := "Failed to execute template \'"
   26 [-]: GETTABLE  R6 R0 K5     ; R6 := R0["name"]
   27 [-]: LOADK     R7 K6        ; R7 := "\'.\n"
   28 [-]: LOADK     R8 K7        ; R8 := "A runtime error occured: "
   29 [-]: GETUPVAL  R9 U6        ; R9 := U6
   30 [-]: TESTSET   R10 R3 1     ; if R3 then R10 := R3 ; goto 33 else goto 32
   31 [-]: JMP       1            ; PC += 1 (goto 33)
   32 [-]: LOADK     R10 K8       ; R10 := "(nil)"
   33 [-]: CALL      R9 2 2       ; R9 := R9(R10)
   34 [-]: CONCAT    R5 R5 R9     ; R5 := concat(R5 to R9)
   35 [-]: CALL      R4 2 1       ;  := R4(R5)
   36 [-]: RETURN    R0 1         ; return 


; Function:        0_3_0
; Defined at line: 90
; #Upvalues:       3
; #Parameters:     2
; Is_vararg:       0
; Max Stack Size:  5

    0 [-]: GETUPVAL  R2 U0        ; R2 := U0
    1 [-]: MOVE      R3 R0        ; R3 := R0
    2 [-]: MOVE      R4 R1        ; R4 := R1
    3 [-]: CALL      R2 3 2       ; R2 := R2(R3 to R4)
    4 [-]: TEST      R2 1         ; if not R2 then goto 6 else goto 13
    5 [-]: JMP       7            ; PC += 7 (goto 13)
    6 [-]: GETUPVAL  R2 U1        ; R2 := U1
    7 [-]: GETTABLE  R2 R2 K0     ; R2 := R2["viewns"]
    8 [-]: GETTABLE  R2 R2 R1     ; R2 := R2[R1]
    9 [-]: TEST      R2 1         ; if not R2 then goto 11 else goto 13
   10 [-]: JMP       2            ; PC += 2 (goto 13)
   11 [-]: GETUPVAL  R2 U2        ; R2 := U2
   12 [-]: GETTABLE  R2 R2 R1     ; R2 := R2[R1]
   13 [-]: RETURN    R2 2         ; return R2
   14 [-]: RETURN    R0 1         ; return 


