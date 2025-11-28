    0 [-]: GETGLOBAL R0 K0        ; R0 := pcall
    1 [-]: GETGLOBAL R1 K1        ; R1 := dofile
    2 [-]: GETGLOBAL R2 K2        ; R2 := _G
    3 [-]: GETGLOBAL R3 K3        ; R3 := module
    4 [-]: LOADK     R4 K4        ; R4 := "luci.version"
    5 [-]: CALL      R3 2 1       ;  := R3(R4)
    6 [-]: MOVE      R3 R0        ; R3 := R0
    7 [-]: MOVE      R4 R1        ; R4 := R1
    8 [-]: LOADK     R5 K5        ; R5 := "/etc/openwrt_release"
    9 [-]: CALL      R3 3 2       ; R3 := R3(R4 to R5)
   10 [-]: TEST      R3 0         ; if R3 then goto 12 else goto 38
   11 [-]: JMP       26           ; PC += 26 (goto 38)
   12 [-]: GETTABLE  R3 R2 K6     ; R3 := R2["DISTRIB_DESCRIPTION"]
   13 [-]: TEST      R3 0         ; if R3 then goto 15 else goto 38
   14 [-]: JMP       23           ; PC += 23 (goto 38)
   15 [-]: LOADK     R3 K8        ; R3 := ""
   16 [-]: SETGLOBAL R3 K7        ; distname := R3
   17 [-]: GETTABLE  R3 R2 K6     ; R3 := R2["DISTRIB_DESCRIPTION"]
   18 [-]: SETGLOBAL R3 K9        ; distversion := R3
   19 [-]: GETTABLE  R3 R2 K10    ; R3 := R2["DISTRIB_REVISION"]
   20 [-]: TEST      R3 0         ; if R3 then goto 22 else goto 42
   21 [-]: JMP       20           ; PC += 20 (goto 42)
   22 [-]: GETTABLE  R3 R2 K10    ; R3 := R2["DISTRIB_REVISION"]
   23 [-]: SETGLOBAL R3 K11       ; distrevision := R3
   24 [-]: GETGLOBAL R3 K9        ; R3 := distversion
   25 [-]: SELF      R3 R3 K12    ; R4 := R3; R3 := R3["find"]
   26 [-]: GETGLOBAL R5 K11       ; R5 := distrevision
   27 [-]: LOADK     R6 K13       ; R6 := Unknown_Type_Error
   28 [-]: LOADBOOL  R7 1 0       ; R7 := true
   29 [-]: CALL      R3 5 2       ; R3 := R3(R4 to R7)
   30 [-]: TEST      R3 1         ; if not R3 then goto 32 else goto 42
   31 [-]: JMP       10           ; PC += 10 (goto 42)
   32 [-]: GETGLOBAL R3 K9        ; R3 := distversion
   33 [-]: LOADK     R4 K14       ; R4 := " "
   34 [-]: GETGLOBAL R5 K11       ; R5 := distrevision
   35 [-]: CONCAT    R3 R3 R5     ; R3 := concat(R3 to R5)
   36 [-]: SETGLOBAL R3 K9        ; distversion := R3
   37 [-]: JMP       4            ; PC += 4 (goto 42)
   38 [-]: LOADK     R3 K15       ; R3 := "OpenWrt"
   39 [-]: SETGLOBAL R3 K7        ; distname := R3
   40 [-]: LOADK     R3 K16       ; R3 := "Development Snapshot"
   41 [-]: SETGLOBAL R3 K9        ; distversion := R3
   42 [-]: LOADK     R3 K18       ; R3 := "LuCI v2.4 branch"
   43 [-]: SETGLOBAL R3 K17       ; luciname := R3
   44 [-]: LOADK     R3 K20       ; R3 := "git-25.240.09623-126633c"
   45 [-]: SETGLOBAL R3 K19       ; luciversion := R3
   46 [-]: RETURN    R0 1         ; return 