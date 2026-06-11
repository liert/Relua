
main <?:0,0> (73 instructions, 292 bytes at 0x4000008b2f30)
0+ params, 6 slots, 0 upvalues, 0 locals, 24 constants, 14 functions
	1	[-]	(null)   	0 -1	; module
	2	[-]	GETTABLE 	1 -2	; "luci.controller.api.xqdatacenter"
	3	[-]	(null)   	2 -3	; package
	4	[-]	TESTSET  	2 2 -4	; "seeall"
	5	[-]	MOD      	0 3 1
	6	[-]	LOADK    	0 0	; 0x74427db70f80
	7	[-]	(null)   	0 -5	; index
	8	[-]	(null)   	0 -6	; require
	9	[-]	GETTABLE 	1 -7	; "luci.http"
	10	[-]	MOD      	0 2 2
	11	[-]	(null)   	1 -6	; require
	12	[-]	GETTABLE 	2 -8	; "json"
	13	[-]	MOD      	1 2 2
	14	[-]	(null)   	2 -6	; require
	15	[-]	GETTABLE 	3 -9	; "xiaoqiang.common.XQConfigs"
	16	[-]	MOD      	2 2 2
	17	[-]	(null)   	3 -6	; require
	18	[-]	GETTABLE 	4 -10	; "xiaoqiang.common.XQFunction"
	19	[-]	MOD      	3 2 2
	20	[-]	(null)   	4 -6	; require
	21	[-]	GETTABLE 	5 -11	; "xiaoqiang.util.XQErrorUtil"
	22	[-]	MOD      	4 2 2
	23	[-]	LOADK    	5 1	; 0x4000008f8420
	24	[-]	CLOSURE  	0 0
	25	[-]	(null)   	5 -12	; fsysProbe
	26	[-]	LOADK    	5 2	; 0x4000008f84a0
	27	[-]	CLOSURE  	0 0
	28	[-]	(null)   	5 -13	; fsysResume
	29	[-]	LOADK    	5 3	; 0x4000008f8520
	30	[-]	CLOSURE  	0 3
	31	[-]	(null)   	5 -14	; chk_param
	32	[-]	LOADK    	5 4	; 0x4000008f85a0
	33	[-]	CLOSURE  	0 0
	34	[-]	CLOSURE  	0 3
	35	[-]	CLOSURE  	0 2
	36	[-]	(null)   	5 -15	; tunnelRequest
	37	[-]	LOADK    	5 5	; 0x4000008f8620
	38	[-]	CLOSURE  	0 3
	39	[-]	CLOSURE  	0 0
	40	[-]	(null)   	5 -16	; identifyDevice
	41	[-]	LOADK    	5 6	; 0x4000008f86a0
	42	[-]	CLOSURE  	0 3
	43	[-]	CLOSURE  	0 0
	44	[-]	(null)   	5 -17	; getDeviceId
	45	[-]	LOADK    	5 7	; 0x4000008f8720
	46	[-]	(null)   	5 -18	; pathEncode
	47	[-]	LOADK    	5 8	; 0x4000008fa820
	48	[-]	CLOSURE  	0 0
	49	[-]	CLOSURE  	0 3
	50	[-]	(null)   	5 -19	; download
	51	[-]	LOADK    	5 9	; 0x4000008fa8a0
	52	[-]	CLOSURE  	0 0
	53	[-]	(null)   	5 -20	; upload
	54	[-]	LOADK    	5 10	; 0x4000008faa20
	55	[-]	CLOSURE  	0 0
	56	[-]	CLOSURE  	0 3
	57	[-]	(null)   	5 -21	; getThumb
	58	[-]	LOADK    	5 11	; 0x4000008faaa0
	59	[-]	CLOSURE  	0 0
	60	[-]	CLOSURE  	0 3
	61	[-]	(null)   	5 -22	; checkFileExist
	62	[-]	LOADK    	5 12	; 0x4000008fe800
	63	[-]	CLOSURE  	0 0
	64	[-]	CLOSURE  	0 3
	65	[-]	CLOSURE  	0 1
	66	[-]	CLOSURE  	0 4
	67	[-]	(null)   	5 -23	; pluginSSH
	68	[-]	LOADK    	5 13	; 0x4000008fe880
	69	[-]	CLOSURE  	0 3
	70	[-]	CLOSURE  	0 4
	71	[-]	CLOSURE  	0 0
	72	[-]	(null)   	5 -24	; pluginSSHStatus
	73	[-]	SETUPVAL 	0 1
constants (24) for 0x4000008b2f30:
	1	"module"
	2	"luci.controller.api.xqdatacenter"
	3	"package"
	4	"seeall"
	5	"index"
	6	"require"
	7	"luci.http"
	8	"json"
	9	"xiaoqiang.common.XQConfigs"
	10	"xiaoqiang.common.XQFunction"
	11	"xiaoqiang.util.XQErrorUtil"
	12	"fsysProbe"
	13	"fsysResume"
	14	"chk_param"
	15	"tunnelRequest"
	16	"identifyDevice"
	17	"getDeviceId"
	18	"pathEncode"
	19	"download"
	20	"upload"
	21	"getThumb"
	22	"checkFileExist"
	23	"pluginSSH"
	24	"pluginSSHStatus"
locals (0) for 0x4000008b2f30:
upvalues (0) for 0x4000008b2f30:

function <?:3,26> (193 instructions, 772 bytes at 0x74427db70f80)
0 params, 8 slots, 0 upvalues, 0 locals, 54 constants, 0 functions
	1	[-]	(null)   	0 -1	; node
	2	[-]	GETTABLE 	1 -2	; "api"
	3	[-]	GETTABLE 	2 -3	; "xqdatacenter"
	4	[-]	MOD      	0 3 2
	5	[-]	(null)   	1 -4	; require
	6	[-]	GETTABLE 	2 -5	; "xiaoqiang.XQFeatures"
	7	[-]	MOD      	1 2 2
	8	[-]	TESTSET  	1 1 -6	; "FEATURES"
	9	[-]	(null)   	2 -8	; firstchild
	10	[-]	MOD      	2 1 2
	11	[-]	POW      	0 -7 2	; "target" -
	12	[-]	POW      	0 -9 -10	; "title" ""
	13	[-]	POW      	0 -11 -12	; "order" 300
	14	[-]	POW      	0 -13 -14	; "sysauth" "admin"
	15	[-]	POW      	0 -15 -16	; "sysauth_authenticator" "jsonauth"
	16	[-]	POW      	0 -17 -18	; "index" true
	17	[-]	(null)   	2 -19	; entry
	18	[-]	CALL     	3 2 0
	19	[-]	GETTABLE 	4 -2	; "api"
	20	[-]	GETTABLE 	5 -3	; "xqdatacenter"
	21	[-]	SETGLOBAL	3 2 1	; 1
	22	[-]	(null)   	4 -8	; firstchild
	23	[-]	MOD      	4 1 2
	24	[-]	(null)   	5 -20	; _
	25	[-]	GETTABLE 	6 -10	; ""
	26	[-]	MOD      	5 2 2
	27	[-]	GETTABLE 	6 -12	; 300
	28	[-]	MOD      	2 5 1
	29	[-]	TESTSET  	2 1 -21	; "apps"
	30	[-]	TESTSET  	2 2 -3	; "xqdatacenter"
	31	[-]	SETTABLE 	2 0 0
	32	[-]	FORLOOP  	160	; to 193
	33	[-]	TESTSET  	2 1 -21	; "apps"
	34	[-]	TESTSET  	2 2 -3	; "xqdatacenter"
	35	[-]	NOT      	0 2 -22	; - "1"
	36	[-]	FORLOOP  	156	; to 193
	37	[-]	(null)   	2 -19	; entry
	38	[-]	CALL     	3 3 0
	39	[-]	GETTABLE 	4 -2	; "api"
	40	[-]	GETTABLE 	5 -3	; "xqdatacenter"
	41	[-]	GETTABLE 	6 -23	; "request"
	42	[-]	SETGLOBAL	3 3 1	; 1
	43	[-]	(null)   	4 -24	; call
	44	[-]	GETTABLE 	5 -25	; "tunnelRequest"
	45	[-]	MOD      	4 2 2
	46	[-]	(null)   	5 -20	; _
	47	[-]	GETTABLE 	6 -10	; ""
	48	[-]	MOD      	5 2 2
	49	[-]	GETTABLE 	6 -26	; 301
	50	[-]	MOD      	2 5 1
	51	[-]	(null)   	2 -19	; entry
	52	[-]	CALL     	3 3 0
	53	[-]	GETTABLE 	4 -2	; "api"
	54	[-]	GETTABLE 	5 -3	; "xqdatacenter"
	55	[-]	GETTABLE 	6 -27	; "identify_device"
	56	[-]	SETGLOBAL	3 3 1	; 1
	57	[-]	(null)   	4 -24	; call
	58	[-]	GETTABLE 	5 -28	; "identifyDevice"
	59	[-]	MOD      	4 2 2
	60	[-]	(null)   	5 -20	; _
	61	[-]	GETTABLE 	6 -10	; ""
	62	[-]	MOD      	5 2 2
	63	[-]	GETTABLE 	6 -29	; 302
	64	[-]	GETTABLE 	7 -30	; 8
	65	[-]	MOD      	2 6 1
	66	[-]	(null)   	2 -19	; entry
	67	[-]	CALL     	3 3 0
	68	[-]	GETTABLE 	4 -2	; "api"
	69	[-]	GETTABLE 	5 -3	; "xqdatacenter"
	70	[-]	GETTABLE 	6 -31	; "download"
	71	[-]	SETGLOBAL	3 3 1	; 1
	72	[-]	(null)   	4 -24	; call
	73	[-]	GETTABLE 	5 -31	; "download"
	74	[-]	MOD      	4 2 2
	75	[-]	(null)   	5 -20	; _
	76	[-]	GETTABLE 	6 -10	; ""
	77	[-]	MOD      	5 2 2
	78	[-]	GETTABLE 	6 -32	; 303
	79	[-]	MOD      	2 5 1
	80	[-]	(null)   	2 -19	; entry
	81	[-]	CALL     	3 3 0
	82	[-]	GETTABLE 	4 -2	; "api"
	83	[-]	GETTABLE 	5 -3	; "xqdatacenter"
	84	[-]	GETTABLE 	6 -33	; "upload"
	85	[-]	SETGLOBAL	3 3 1	; 1
	86	[-]	(null)   	4 -24	; call
	87	[-]	GETTABLE 	5 -33	; "upload"
	88	[-]	MOD      	4 2 2
	89	[-]	(null)   	5 -20	; _
	90	[-]	GETTABLE 	6 -10	; ""
	91	[-]	MOD      	5 2 2
	92	[-]	GETTABLE 	6 -34	; 304
	93	[-]	GETTABLE 	7 -35	; 16
	94	[-]	MOD      	2 6 1
	95	[-]	(null)   	2 -19	; entry
	96	[-]	CALL     	3 3 0
	97	[-]	GETTABLE 	4 -2	; "api"
	98	[-]	GETTABLE 	5 -3	; "xqdatacenter"
	99	[-]	GETTABLE 	6 -36	; "thumb"
	100	[-]	SETGLOBAL	3 3 1	; 1
	101	[-]	(null)   	4 -24	; call
	102	[-]	GETTABLE 	5 -37	; "getThumb"
	103	[-]	MOD      	4 2 2
	104	[-]	(null)   	5 -20	; _
	105	[-]	GETTABLE 	6 -10	; ""
	106	[-]	MOD      	5 2 2
	107	[-]	GETTABLE 	6 -38	; 305
	108	[-]	MOD      	2 5 1
	109	[-]	(null)   	2 -19	; entry
	110	[-]	CALL     	3 3 0
	111	[-]	GETTABLE 	4 -2	; "api"
	112	[-]	GETTABLE 	5 -3	; "xqdatacenter"
	113	[-]	GETTABLE 	6 -39	; "device_id"
	114	[-]	SETGLOBAL	3 3 1	; 1
	115	[-]	(null)   	4 -24	; call
	116	[-]	GETTABLE 	5 -40	; "getDeviceId"
	117	[-]	MOD      	4 2 2
	118	[-]	(null)   	5 -20	; _
	119	[-]	GETTABLE 	6 -10	; ""
	120	[-]	MOD      	5 2 2
	121	[-]	GETTABLE 	6 -41	; 306
	122	[-]	MOD      	2 5 1
	123	[-]	(null)   	2 -19	; entry
	124	[-]	CALL     	3 3 0
	125	[-]	GETTABLE 	4 -2	; "api"
	126	[-]	GETTABLE 	5 -3	; "xqdatacenter"
	127	[-]	GETTABLE 	6 -42	; "check_file_exist"
	128	[-]	SETGLOBAL	3 3 1	; 1
	129	[-]	(null)   	4 -24	; call
	130	[-]	GETTABLE 	5 -43	; "checkFileExist"
	131	[-]	MOD      	4 2 2
	132	[-]	(null)   	5 -20	; _
	133	[-]	GETTABLE 	6 -10	; ""
	134	[-]	MOD      	5 2 2
	135	[-]	GETTABLE 	6 -44	; 307
	136	[-]	MOD      	2 5 1
	137	[-]	(null)   	2 -19	; entry
	138	[-]	CALL     	3 3 0
	139	[-]	GETTABLE 	4 -2	; "api"
	140	[-]	GETTABLE 	5 -3	; "xqdatacenter"
	141	[-]	GETTABLE 	6 -45	; "plugin_ssh"
	142	[-]	SETGLOBAL	3 3 1	; 1
	143	[-]	(null)   	4 -24	; call
	144	[-]	GETTABLE 	5 -46	; "pluginSSH"
	145	[-]	MOD      	4 2 2
	146	[-]	(null)   	5 -20	; _
	147	[-]	GETTABLE 	6 -10	; ""
	148	[-]	MOD      	5 2 2
	149	[-]	GETTABLE 	6 -47	; 308
	150	[-]	MOD      	2 5 1
	151	[-]	(null)   	2 -19	; entry
	152	[-]	CALL     	3 3 0
	153	[-]	GETTABLE 	4 -2	; "api"
	154	[-]	GETTABLE 	5 -3	; "xqdatacenter"
	155	[-]	GETTABLE 	6 -48	; "plugin_ssh_status"
	156	[-]	SETGLOBAL	3 3 1	; 1
	157	[-]	(null)   	4 -24	; call
	158	[-]	GETTABLE 	5 -49	; "pluginSSHStatus"
	159	[-]	MOD      	4 2 2
	160	[-]	(null)   	5 -20	; _
	161	[-]	GETTABLE 	6 -10	; ""
	162	[-]	MOD      	5 2 2
	163	[-]	GETTABLE 	6 -50	; 309
	164	[-]	MOD      	2 5 1
	165	[-]	(null)   	2 -19	; entry
	166	[-]	CALL     	3 3 0
	167	[-]	GETTABLE 	4 -2	; "api"
	168	[-]	GETTABLE 	5 -3	; "xqdatacenter"
	169	[-]	GETTABLE 	6 -51	; "fsys_probe"
	170	[-]	SETGLOBAL	3 3 1	; 1
	171	[-]	(null)   	4 -24	; call
	172	[-]	GETTABLE 	5 -52	; "fsysProbe"
	173	[-]	MOD      	4 2 2
	174	[-]	(null)   	5 -20	; _
	175	[-]	GETTABLE 	6 -10	; ""
	176	[-]	MOD      	5 2 2
	177	[-]	GETTABLE 	6 -26	; 301
	178	[-]	MOD      	2 5 1
	179	[-]	(null)   	2 -19	; entry
	180	[-]	CALL     	3 3 0
	181	[-]	GETTABLE 	4 -2	; "api"
	182	[-]	GETTABLE 	5 -3	; "xqdatacenter"
	183	[-]	GETTABLE 	6 -53	; "fsys_resume"
	184	[-]	SETGLOBAL	3 3 1	; 1
	185	[-]	(null)   	4 -24	; call
	186	[-]	GETTABLE 	5 -54	; "fsysResume"
	187	[-]	MOD      	4 2 2
	188	[-]	(null)   	5 -20	; _
	189	[-]	GETTABLE 	6 -10	; ""
	190	[-]	MOD      	5 2 2
	191	[-]	GETTABLE 	6 -26	; 301
	192	[-]	MOD      	2 5 1
	193	[-]	SETUPVAL 	0 1
constants (54) for 0x74427db70f80:
	1	"node"
	2	"api"
	3	"xqdatacenter"
	4	"require"
	5	"xiaoqiang.XQFeatures"
	6	"FEATURES"
	7	"target"
	8	"firstchild"
	9	"title"
	10	""
	11	"order"
	12	300
	13	"sysauth"
	14	"admin"
	15	"sysauth_authenticator"
	16	"jsonauth"
	17	"index"
	18	true
	19	"entry"
	20	"_"
	21	"apps"
	22	"1"
	23	"request"
	24	"call"
	25	"tunnelRequest"
	26	301
	27	"identify_device"
	28	"identifyDevice"
	29	302
	30	8
	31	"download"
	32	303
	33	"upload"
	34	304
	35	16
	36	"thumb"
	37	"getThumb"
	38	305
	39	"device_id"
	40	"getDeviceId"
	41	306
	42	"check_file_exist"
	43	"checkFileExist"
	44	307
	45	"plugin_ssh"
	46	"pluginSSH"
	47	308
	48	"plugin_ssh_status"
	49	"pluginSSHStatus"
	50	309
	51	"fsys_probe"
	52	"fsysProbe"
	53	"fsys_resume"
	54	"fsysResume"
locals (0) for 0x74427db70f80:
upvalues (0) for 0x74427db70f80:

function <?:34,50> (33 instructions, 132 bytes at 0x4000008f8420)
0 params, 5 slots, 1 upvalue, 0 locals, 18 constants, 0 functions
	1	[-]	CALL     	0 0 0
	2	[-]	POW      	0 -1 -2	; "code" 0
	3	[-]	POW      	0 -3 -4	; "msg" ""
	4	[-]	(null)   	1 -5	; require
	5	[-]	GETTABLE 	2 -6	; "xiaoqiang.module.XQDisk"
	6	[-]	MOD      	1 2 2
	7	[-]	(null)   	2 -7	; tonumber
	8	[-]	UNM      	3 0	; -
	9	[-]	TESTSET  	3 3 -8	; "formvalue"
	10	[-]	GETTABLE 	4 -9	; "type"
	11	[-]	MOD      	3 2 2
	12	[-]	SETTABLE 	3 0 1
	13	[-]	FORLOOP  	1	; to 15
	14	[-]	GETTABLE 	3 -10	; 3
	15	[-]	MOD      	2 2 2
	16	[-]	NOT      	0 2 -11	; - 1
	17	[-]	FORLOOP  	3	; to 21
	18	[-]	TESTSET  	3 1 -12	; "disk_check"
	19	[-]	MOD      	3 1 1
	20	[-]	FORLOOP  	8	; to 29
	21	[-]	NOT      	0 2 -13	; - 2
	22	[-]	FORLOOP  	4	; to 27
	23	[-]	TESTSET  	3 1 -15	; "get_diskstatus"
	24	[-]	MOD      	3 1 2
	25	[-]	POW      	0 -14 3	; "status" -
	26	[-]	FORLOOP  	2	; to 29
	27	[-]	POW      	0 -1 -16	; "code" 6
	28	[-]	POW      	0 -3 -17	; "msg" "ParameterError"
	29	[-]	UNM      	3 0	; -
	30	[-]	TESTSET  	3 3 -18	; "write_json"
	31	[-]	CLOSURE  	4 0
	32	[-]	MOD      	3 2 1
	33	[-]	SETUPVAL 	0 1
constants (18) for 0x4000008f8420:
	1	"code"
	2	0
	3	"msg"
	4	""
	5	"require"
	6	"xiaoqiang.module.XQDisk"
	7	"tonumber"
	8	"formvalue"
	9	"type"
	10	3
	11	1
	12	"disk_check"
	13	2
	14	"status"
	15	"get_diskstatus"
	16	6
	17	"ParameterError"
	18	"write_json"
locals (0) for 0x4000008f8420:
upvalues (0) for 0x4000008f8420:

function <?:52,67> (33 instructions, 132 bytes at 0x4000008f84a0)
0 params, 5 slots, 1 upvalue, 0 locals, 18 constants, 0 functions
	1	[-]	CALL     	0 0 0
	2	[-]	POW      	0 -1 -2	; "code" 0
	3	[-]	POW      	0 -3 -4	; "msg" ""
	4	[-]	(null)   	1 -5	; require
	5	[-]	GETTABLE 	2 -6	; "xiaoqiang.module.XQDisk"
	6	[-]	MOD      	1 2 2
	7	[-]	(null)   	2 -7	; tonumber
	8	[-]	UNM      	3 0	; -
	9	[-]	TESTSET  	3 3 -8	; "formvalue"
	10	[-]	GETTABLE 	4 -9	; "type"
	11	[-]	MOD      	3 2 2
	12	[-]	SETTABLE 	3 0 1
	13	[-]	FORLOOP  	1	; to 15
	14	[-]	GETTABLE 	3 -10	; 3
	15	[-]	MOD      	2 2 2
	16	[-]	NOT      	0 2 -11	; - 1
	17	[-]	FORLOOP  	3	; to 21
	18	[-]	TESTSET  	3 1 -12	; "disk_repair"
	19	[-]	MOD      	3 1 1
	20	[-]	FORLOOP  	8	; to 29
	21	[-]	NOT      	0 2 -13	; - 2
	22	[-]	FORLOOP  	4	; to 27
	23	[-]	TESTSET  	3 1 -15	; "get_repairstatus"
	24	[-]	MOD      	3 1 2
	25	[-]	POW      	0 -14 3	; "status" -
	26	[-]	FORLOOP  	2	; to 29
	27	[-]	POW      	0 -1 -16	; "code" 6
	28	[-]	POW      	0 -3 -17	; "msg" "ParameterError"
	29	[-]	UNM      	3 0	; -
	30	[-]	TESTSET  	3 3 -18	; "write_json"
	31	[-]	CLOSURE  	4 0
	32	[-]	MOD      	3 2 1
	33	[-]	SETUPVAL 	0 1
constants (18) for 0x4000008f84a0:
	1	"code"
	2	0
	3	"msg"
	4	""
	5	"require"
	6	"xiaoqiang.module.XQDisk"
	7	"tonumber"
	8	"formvalue"
	9	"type"
	10	3
	11	1
	12	"disk_repair"
	13	2
	14	"status"
	15	"get_repairstatus"
	16	6
	17	"ParameterError"
	18	"write_json"
locals (0) for 0x4000008f84a0:
upvalues (0) for 0x4000008f84a0:

function <?:69,109> (109 instructions, 436 bytes at 0x4000008f8520)
1 param, 11 slots, 1 upvalue, 0 locals, 23 constants, 0 functions
	1	[-]	(null)   	1 -1	; require
	2	[-]	GETTABLE 	2 -2	; "xiaoqiang.XQLog"
	3	[-]	MOD      	1 2 2
	4	[-]	(null)   	2 -1	; require
	5	[-]	GETTABLE 	3 -3	; "json"
	6	[-]	MOD      	2 2 2
	7	[-]	TESTSET  	3 2 -4	; "decode"
	8	[-]	CLOSURE  	4 0
	9	[-]	MOD      	3 2 2
	10	[-]	(null)   	4 -1	; require
	11	[-]	GETTABLE 	5 -5	; "xiaoqiang.util.XQSecureUtil"
	12	[-]	MOD      	4 2 2
	13	[-]	(null)   	5 -6	; type
	14	[-]	CLOSURE  	6 3
	15	[-]	MOD      	5 2 2
	16	[-]	LEN      	1 5 -7
	17	[-]	FORLOOP  	6	; to 24
	18	[-]	TESTSET  	5 1 -8	; "log"
	19	[-]	GETTABLE 	6 -9	; 6
	20	[-]	GETTABLE 	7 -10	; "chk_param: wrong type"
	21	[-]	MOD      	5 3 1
	22	[-]	LT       	5 0 0
	23	[-]	SETUPVAL 	5 2
	24	[-]	TESTSET  	5 3 -11	; "api"
	25	[-]	NOT      	0 5 -12	; - 63
	26	[-]	FORLOOP  	27	; to 54
	27	[-]	TESTSET  	5 3 -13	; "path"
	28	[-]	UNM      	6 0	; -
	29	[-]	TESTSET  	6 6 -14	; "isStrNil"
	30	[-]	CLOSURE  	7 5
	31	[-]	MOD      	6 2 2
	32	[-]	SETTABLE 	6 0 0
	33	[-]	FORLOOP  	6	; to 40
	34	[-]	TESTSET  	6 1 -8	; "log"
	35	[-]	GETTABLE 	7 -9	; 6
	36	[-]	GETTABLE 	8 -15	; "chk_param:  path is null"
	37	[-]	MOD      	6 3 1
	38	[-]	LT       	6 0 0
	39	[-]	SETUPVAL 	6 2
	40	[-]	TESTSET  	6 4 -16	; "hackCharsCheck"
	41	[-]	CLOSURE  	7 5
	42	[-]	MOD      	6 2 2
	43	[-]	NOT      	0 6 -17	; - ""
	44	[-]	FORLOOP  	6	; to 51
	45	[-]	TESTSET  	7 1 -8	; "log"
	46	[-]	GETTABLE 	8 -9	; 6
	47	[-]	GETTABLE 	9 -18	; "chk_param: path has invalid char"
	48	[-]	MOD      	7 3 1
	49	[-]	LT       	7 0 0
	50	[-]	SETUPVAL 	7 2
	51	[-]	LT       	7 1 0
	52	[-]	SETUPVAL 	7 2
	53	[-]	FORLOOP  	53	; to 107
	54	[-]	TESTSET  	5 3 -11	; "api"
	55	[-]	NOT      	0 5 -19	; - 7
	56	[-]	FORLOOP  	50	; to 107
	57	[-]	TESTSET  	5 3 -20	; "vendor"
	58	[-]	TESTSET  	6 3 -21	; "dev"
	59	[-]	TESTSET  	7 3 -6	; "type"
	60	[-]	UNM      	8 0	; -
	61	[-]	TESTSET  	8 8 -14	; "isStrNil"
	62	[-]	CLOSURE  	9 5
	63	[-]	MOD      	8 2 2
	64	[-]	SETTABLE 	8 0 1
	65	[-]	FORLOOP  	12	; to 78
	66	[-]	UNM      	8 0	; -
	67	[-]	TESTSET  	8 8 -14	; "isStrNil"
	68	[-]	CLOSURE  	9 6
	69	[-]	MOD      	8 2 2
	70	[-]	SETTABLE 	8 0 1
	71	[-]	FORLOOP  	6	; to 78
	72	[-]	UNM      	8 0	; -
	73	[-]	TESTSET  	8 8 -14	; "isStrNil"
	74	[-]	CLOSURE  	9 7
	75	[-]	MOD      	8 2 2
	76	[-]	SETTABLE 	8 0 0
	77	[-]	FORLOOP  	6	; to 84
	78	[-]	TESTSET  	8 1 -8	; "log"
	79	[-]	GETTABLE 	9 -9	; 6
	80	[-]	GETTABLE 	10 -22	; "chk_param: param is null"
	81	[-]	MOD      	8 3 1
	82	[-]	LT       	8 0 0
	83	[-]	SETUPVAL 	8 2
	84	[-]	TESTSET  	8 4 -16	; "hackCharsCheck"
	85	[-]	CLOSURE  	9 5
	86	[-]	MOD      	8 2 2
	87	[-]	NOT      	1 8 -17	; - ""
	88	[-]	FORLOOP  	10	; to 99
	89	[-]	TESTSET  	8 4 -16	; "hackCharsCheck"
	90	[-]	CLOSURE  	9 6
	91	[-]	MOD      	8 2 2
	92	[-]	NOT      	1 8 -17	; - ""
	93	[-]	FORLOOP  	5	; to 99
	94	[-]	TESTSET  	8 4 -16	; "hackCharsCheck"
	95	[-]	CLOSURE  	9 7
	96	[-]	MOD      	8 2 2
	97	[-]	NOT      	0 8 -17	; - ""
	98	[-]	FORLOOP  	6	; to 105
	99	[-]	TESTSET  	8 1 -8	; "log"
	100	[-]	GETTABLE 	9 -9	; 6
	101	[-]	GETTABLE 	10 -23	; "chk_param: params has invalid char"
	102	[-]	MOD      	8 3 1
	103	[-]	LT       	8 0 0
	104	[-]	SETUPVAL 	8 2
	105	[-]	LT       	8 1 0
	106	[-]	SETUPVAL 	8 2
	107	[-]	LT       	5 1 0
	108	[-]	SETUPVAL 	5 2
	109	[-]	SETUPVAL 	0 1
constants (23) for 0x4000008f8520:
	1	"require"
	2	"xiaoqiang.XQLog"
	3	"json"
	4	"decode"
	5	"xiaoqiang.util.XQSecureUtil"
	6	"type"
	7	"table"
	8	"log"
	9	6
	10	"chk_param: wrong type"
	11	"api"
	12	63
	13	"path"
	14	"isStrNil"
	15	"chk_param:  path is null"
	16	"hackCharsCheck"
	17	""
	18	"chk_param: path has invalid char"
	19	7
	20	"vendor"
	21	"dev"
	22	"chk_param: param is null"
	23	"chk_param: params has invalid char"
locals (0) for 0x4000008f8520:
upvalues (0) for 0x4000008f8520:

function <?:111,128> (47 instructions, 188 bytes at 0x4000008f85a0)
0 params, 11 slots, 3 upvalues, 0 locals, 16 constants, 0 functions
	1	[-]	(null)   	0 -1	; require
	2	[-]	GETTABLE 	1 -2	; "luci.util"
	3	[-]	MOD      	0 2 2
	4	[-]	(null)   	1 -1	; require
	5	[-]	GETTABLE 	2 -3	; "xiaoqiang.util.XQCryptoUtil"
	6	[-]	MOD      	1 2 2
	7	[-]	UNM      	2 0	; -
	8	[-]	TESTSET  	2 2 -4	; "formvalue_unsafe"
	9	[-]	GETTABLE 	3 -5	; "payload"
	10	[-]	MOD      	2 2 2
	11	[-]	CALL     	3 0 0
	12	[-]	UNM      	4 1	; -
	13	[-]	TESTSET  	4 4 -6	; "isStrNil"
	14	[-]	CLOSURE  	5 2
	15	[-]	MOD      	4 2 2
	16	[-]	SETTABLE 	4 0 1
	17	[-]	FORLOOP  	5	; to 23
	18	[-]	(null)   	4 -7	; chk_param
	19	[-]	CLOSURE  	5 2
	20	[-]	MOD      	4 2 2
	21	[-]	SETTABLE 	4 0 1
	22	[-]	FORLOOP  	7	; to 30
	23	[-]	POW      	3 -8 -9	; "code" 6
	24	[-]	POW      	3 -10 -11	; "msg" "ParameterError"
	25	[-]	UNM      	4 0	; -
	26	[-]	TESTSET  	4 4 -12	; "write_json"
	27	[-]	CLOSURE  	5 3
	28	[-]	MOD      	4 2 1
	29	[-]	SETUPVAL 	0 1
	30	[-]	TESTSET  	4 1 -13	; "binaryBase64Enc"
	31	[-]	CLOSURE  	5 2
	32	[-]	MOD      	4 2 2
	33	[-]	UNM      	5 2	; -
	34	[-]	TESTSET  	5 5 -14	; "THRIFT_TUNNEL_TO_DATACENTER"
	35	[-]	LE       	5 5 4
	36	[-]	TESTSET  	6 0 -15	; "exec"
	37	[-]	CLOSURE  	7 5
	38	[-]	MOD      	6 2 2
	39	[-]	CLOSURE  	3 6
	40	[-]	UNM      	6 0	; -
	41	[-]	TESTSET  	6 6 -16	; "write"
	42	[-]	CLOSURE  	7 3
	43	[-]	(null)   	8 8
	44	[-]	LT       	9 0 0
	45	[-]	LT       	10 1 0
	46	[-]	MOD      	6 5 1
	47	[-]	SETUPVAL 	0 1
constants (16) for 0x4000008f85a0:
	1	"require"
	2	"luci.util"
	3	"xiaoqiang.util.XQCryptoUtil"
	4	"formvalue_unsafe"
	5	"payload"
	6	"isStrNil"
	7	"chk_param"
	8	"code"
	9	6
	10	"msg"
	11	"ParameterError"
	12	"write_json"
	13	"binaryBase64Enc"
	14	"THRIFT_TUNNEL_TO_DATACENTER"
	15	"exec"
	16	"write"
locals (0) for 0x4000008f85a0:
upvalues (0) for 0x4000008f85a0:

function <?:130,135> (11 instructions, 44 bytes at 0x4000008f8620)
0 params, 3 slots, 2 upvalues, 0 locals, 5 constants, 0 functions
	1	[-]	CALL     	0 0 0
	2	[-]	POW      	0 -1 -2	; "code" 0
	3	[-]	UNM      	1 0	; -
	4	[-]	TESTSET  	1 1 -4	; "mattool_identify_device"
	5	[-]	MOD      	1 1 2
	6	[-]	POW      	0 -3 1	; "info" -
	7	[-]	UNM      	1 1	; -
	8	[-]	TESTSET  	1 1 -5	; "write_json"
	9	[-]	CLOSURE  	2 0
	10	[-]	MOD      	1 2 1
	11	[-]	SETUPVAL 	0 1
constants (5) for 0x4000008f8620:
	1	"code"
	2	0
	3	"info"
	4	"mattool_identify_device"
	5	"write_json"
locals (0) for 0x4000008f8620:
upvalues (0) for 0x4000008f8620:

function <?:137,142> (11 instructions, 44 bytes at 0x4000008f86a0)
0 params, 3 slots, 2 upvalues, 0 locals, 5 constants, 0 functions
	1	[-]	CALL     	0 0 0
	2	[-]	POW      	0 -1 -2	; "code" 0
	3	[-]	UNM      	1 0	; -
	4	[-]	TESTSET  	1 1 -4	; "mattool_get_deviceid"
	5	[-]	MOD      	1 1 2
	6	[-]	POW      	0 -3 1	; "deviceId" -
	7	[-]	UNM      	1 1	; -
	8	[-]	TESTSET  	1 1 -5	; "write_json"
	9	[-]	CLOSURE  	2 0
	10	[-]	MOD      	1 2 1
	11	[-]	SETUPVAL 	0 1
constants (5) for 0x4000008f86a0:
	1	"code"
	2	0
	3	"deviceId"
	4	"mattool_get_deviceid"
	5	"write_json"
locals (0) for 0x4000008f86a0:
upvalues (0) for 0x4000008f86a0:

function <?:144,151> (17 instructions, 68 bytes at 0x4000008f8720)
1 param, 5 slots, 0 upvalues, 0 locals, 8 constants, 0 functions
	1	[-]	(null)   	1 -2	; require
	2	[-]	GETTABLE 	2 -1	; "lcurl"
	3	[-]	MOD      	1 2 2
	4	[-]	(null)   	1 -1	; lcurl
	5	[-]	(null)   	1 -3	; string
	6	[-]	TESTSET  	1 1 -4	; "gsub"
	7	[-]	(null)   	2 -1	; lcurl
	8	[-]	TESTSET  	2 2 -5	; "easy"
	9	[-]	MOD      	2 1 2
	10	[-]	DIV      	2 2 -6	; "escape"
	11	[-]	CLOSURE  	4 0
	12	[-]	MOD      	2 3 2
	13	[-]	GETTABLE 	3 -7	; "%%2F"
	14	[-]	GETTABLE 	4 -8	; "/"
	15	[-]	SUB      	1 4 0
	16	[-]	SETUPVAL 	1 0
	17	[-]	SETUPVAL 	0 1
constants (8) for 0x4000008f8720:
	1	"lcurl"
	2	"require"
	3	"string"
	4	"gsub"
	5	"easy"
	6	"escape"
	7	"%%2F"
	8	"/"
locals (0) for 0x4000008f8720:
upvalues (0) for 0x4000008f8720:

function <?:153,253> (377 instructions, 1508 bytes at 0x4000008fa820)
0 params, 22 slots, 2 upvalues, 0 locals, 59 constants, 0 functions
	1	[-]	(null)   	0 -1	; require
	2	[-]	GETTABLE 	1 -2	; "nixio.fs"
	3	[-]	MOD      	0 2 2
	4	[-]	(null)   	1 -1	; require
	5	[-]	GETTABLE 	2 -3	; "luci.http.protocol.mime"
	6	[-]	MOD      	1 2 2
	7	[-]	(null)   	2 -1	; require
	8	[-]	GETTABLE 	3 -4	; "luci.ltn12"
	9	[-]	MOD      	2 2 2
	10	[-]	(null)   	3 -1	; require
	11	[-]	GETTABLE 	4 -5	; "xiaoqiang.XQLog"
	12	[-]	MOD      	3 2 2
	13	[-]	UNM      	4 0	; -
	14	[-]	TESTSET  	4 4 -6	; "formvalue"
	15	[-]	GETTABLE 	5 -7	; "path"
	16	[-]	LT       	6 0 0
	17	[-]	GETTABLE 	7 -8	; "string"
	18	[-]	MOD      	4 4 2
	19	[-]	UNM      	5 1	; -
	20	[-]	TESTSET  	5 5 -9	; "isStrNil"
	21	[-]	CLOSURE  	6 4
	22	[-]	MOD      	5 2 2
	23	[-]	SETTABLE 	5 0 0
	24	[-]	FORLOOP  	8	; to 33
	25	[-]	UNM      	5 0	; -
	26	[-]	TESTSET  	5 5 -10	; "status"
	27	[-]	GETTABLE 	6 -11	; 404
	28	[-]	(null)   	7 -12	; _
	29	[-]	GETTABLE 	8 -13	; "no Such file"
	30	[-]	MOD      	7 2 0
	31	[-]	MOD      	5 0 1
	32	[-]	SETUPVAL 	0 1
	33	[-]	GETTABLE 	5 -14	; "/userdisk/data/"
	34	[-]	GETTABLE 	6 -15	; "/mnt/"
	35	[-]	GETTABLE 	7 -16	; "/userdisk/privacyData/"
	36	[-]	GETTABLE 	8 -17	; "/userdisk/appdata/"
	37	[-]	GETTABLE 	9 -18	; "/userdisk/.thumbnails/"
	38	[-]	(null)   	10 -8	; string
	39	[-]	TESTSET  	10 10 -19	; "sub"
	40	[-]	CLOSURE  	11 4
	41	[-]	GETTABLE 	12 -20	; 1
	42	[-]	(null)   	13 -8	; string
	43	[-]	TESTSET  	13 13 -21	; "len"
	44	[-]	CLOSURE  	14 5
	45	[-]	MOD      	13 2 0
	46	[-]	MOD      	10 0 2
	47	[-]	LEN      	1 10 5
	48	[-]	FORLOOP  	52	; to 101
	49	[-]	(null)   	10 -8	; string
	50	[-]	TESTSET  	10 10 -19	; "sub"
	51	[-]	CLOSURE  	11 4
	52	[-]	GETTABLE 	12 -20	; 1
	53	[-]	(null)   	13 -8	; string
	54	[-]	TESTSET  	13 13 -21	; "len"
	55	[-]	CLOSURE  	14 6
	56	[-]	MOD      	13 2 0
	57	[-]	MOD      	10 0 2
	58	[-]	LEN      	1 10 6
	59	[-]	FORLOOP  	41	; to 101
	60	[-]	(null)   	10 -8	; string
	61	[-]	TESTSET  	10 10 -19	; "sub"
	62	[-]	CLOSURE  	11 4
	63	[-]	GETTABLE 	12 -20	; 1
	64	[-]	(null)   	13 -8	; string
	65	[-]	TESTSET  	13 13 -21	; "len"
	66	[-]	CLOSURE  	14 7
	67	[-]	MOD      	13 2 0
	68	[-]	MOD      	10 0 2
	69	[-]	LEN      	1 10 7
	70	[-]	FORLOOP  	30	; to 101
	71	[-]	(null)   	10 -8	; string
	72	[-]	TESTSET  	10 10 -19	; "sub"
	73	[-]	CLOSURE  	11 4
	74	[-]	GETTABLE 	12 -20	; 1
	75	[-]	(null)   	13 -8	; string
	76	[-]	TESTSET  	13 13 -21	; "len"
	77	[-]	CLOSURE  	14 8
	78	[-]	MOD      	13 2 0
	79	[-]	MOD      	10 0 2
	80	[-]	LEN      	1 10 8
	81	[-]	FORLOOP  	19	; to 101
	82	[-]	(null)   	10 -8	; string
	83	[-]	TESTSET  	10 10 -19	; "sub"
	84	[-]	CLOSURE  	11 4
	85	[-]	GETTABLE 	12 -20	; 1
	86	[-]	(null)   	13 -8	; string
	87	[-]	TESTSET  	13 13 -21	; "len"
	88	[-]	CLOSURE  	14 9
	89	[-]	MOD      	13 2 0
	90	[-]	MOD      	10 0 2
	91	[-]	LEN      	1 10 9
	92	[-]	FORLOOP  	8	; to 101
	93	[-]	UNM      	10 0	; -
	94	[-]	TESTSET  	10 10 -10	; "status"
	95	[-]	GETTABLE 	11 -22	; 403
	96	[-]	(null)   	12 -12	; _
	97	[-]	GETTABLE 	13 -23	; "no permission"
	98	[-]	MOD      	12 2 0
	99	[-]	MOD      	10 0 1
	100	[-]	SETUPVAL 	0 1
	101	[-]	TESTSET  	10 3 -24	; "log"
	102	[-]	GETTABLE 	11 -25	; 7
	103	[-]	GETTABLE 	12 -26	; "=============path = "
	104	[-]	CLOSURE  	13 4
	105	[-]	CONCAT   	12 12 13
	106	[-]	MOD      	10 3 1
	107	[-]	(null)   	10 -8	; string
	108	[-]	TESTSET  	10 10 -27	; "find"
	109	[-]	CLOSURE  	11 4
	110	[-]	GETTABLE 	12 -28	; "/%.%./"
	111	[-]	MOD      	10 3 2
	112	[-]	SETTABLE 	10 0 0
	113	[-]	FORLOOP  	8	; to 122
	114	[-]	UNM      	10 0	; -
	115	[-]	TESTSET  	10 10 -10	; "status"
	116	[-]	GETTABLE 	11 -11	; 404
	117	[-]	(null)   	12 -12	; _
	118	[-]	GETTABLE 	13 -13	; "no Such file"
	119	[-]	MOD      	12 2 0
	120	[-]	MOD      	10 0 1
	121	[-]	SETUPVAL 	0 1
	122	[-]	TESTSET  	10 0 -29	; "stat"
	123	[-]	CLOSURE  	11 4
	124	[-]	MOD      	10 2 2
	125	[-]	SETTABLE 	10 0 1
	126	[-]	FORLOOP  	8	; to 135
	127	[-]	UNM      	11 0	; -
	128	[-]	TESTSET  	11 11 -10	; "status"
	129	[-]	GETTABLE 	12 -11	; 404
	130	[-]	(null)   	13 -12	; _
	131	[-]	GETTABLE 	14 -13	; "no Such file"
	132	[-]	MOD      	13 2 0
	133	[-]	MOD      	11 0 1
	134	[-]	SETUPVAL 	0 1
	135	[-]	UNM      	11 0	; -
	136	[-]	TESTSET  	11 11 -30	; "header"
	137	[-]	GETTABLE 	12 -31	; "Accept-Ranges"
	138	[-]	GETTABLE 	13 -32	; "bytes"
	139	[-]	MOD      	11 3 1
	140	[-]	UNM      	11 0	; -
	141	[-]	TESTSET  	11 11 -30	; "header"
	142	[-]	GETTABLE 	12 -33	; "Content-Type"
	143	[-]	TESTSET  	13 1 -34	; "to_mime"
	144	[-]	CLOSURE  	14 4
	145	[-]	MOD      	13 2 0
	146	[-]	MOD      	11 0 1
	147	[-]	UNM      	11 0	; -
	148	[-]	TESTSET  	11 11 -35	; "getenv"
	149	[-]	GETTABLE 	12 -36	; "HTTP_RANGE"
	150	[-]	MOD      	11 2 2
	151	[-]	SETTABLE 	11 0 0
	152	[-]	FORLOOP  	19	; to 172
	153	[-]	UNM      	12 0	; -
	154	[-]	TESTSET  	12 12 -10	; "status"
	155	[-]	GETTABLE 	13 -37	; 206
	156	[-]	MOD      	12 2 1
	157	[-]	(null)   	12 -8	; string
	158	[-]	TESTSET  	12 12 -38	; "gsub"
	159	[-]	CLOSURE  	13 11
	160	[-]	GETTABLE 	14 -39	; "bytes="
	161	[-]	GETTABLE 	15 -40	; ""
	162	[-]	MOD      	12 4 2
	163	[-]	CLOSURE  	11 12
	164	[-]	(null)   	12 -8	; string
	165	[-]	TESTSET  	12 12 -38	; "gsub"
	166	[-]	CLOSURE  	13 11
	167	[-]	GETTABLE 	14 -41	; "-"
	168	[-]	GETTABLE 	15 -40	; ""
	169	[-]	MOD      	12 4 2
	170	[-]	CLOSURE  	11 12
	171	[-]	FORLOOP  	1	; to 173
	172	[-]	GETTABLE 	11 -42	; 0
	173	[-]	TESTSET  	12 3 -24	; "log"
	174	[-]	GETTABLE 	13 -25	; 7
	175	[-]	GETTABLE 	14 -43	; "=============range = "
	176	[-]	CLOSURE  	15 11
	177	[-]	CONCAT   	14 14 15
	178	[-]	MOD      	12 3 1
	179	[-]	GETTABLE 	12 -44	; "bytes "
	180	[-]	CLOSURE  	13 11
	181	[-]	GETTABLE 	14 -41	; "-"
	182	[-]	TESTSET  	15 10 -45	; "size"
	183	[-]	ADD      	15 15 -20	; - 1
	184	[-]	GETTABLE 	16 -46	; "/"
	185	[-]	TESTSET  	17 10 -45	; "size"
	186	[-]	CONCAT   	12 12 17
	187	[-]	TESTSET  	13 3 -24	; "log"
	188	[-]	GETTABLE 	14 -25	; 7
	189	[-]	GETTABLE 	15 -47	; "=============contentRange = "
	190	[-]	CLOSURE  	16 12
	191	[-]	CONCAT   	15 15 16
	192	[-]	MOD      	13 3 1
	193	[-]	UNM      	13 0	; -
	194	[-]	TESTSET  	13 13 -30	; "header"
	195	[-]	GETTABLE 	14 -48	; "Content-Length"
	196	[-]	TESTSET  	15 10 -45	; "size"
	197	[-]	ADD      	15 15 11
	198	[-]	MOD      	13 3 1
	199	[-]	UNM      	13 0	; -
	200	[-]	TESTSET  	13 13 -30	; "header"
	201	[-]	GETTABLE 	14 -49	; "Content-Range"
	202	[-]	CLOSURE  	15 12
	203	[-]	MOD      	13 3 1
	204	[-]	UNM      	13 0	; -
	205	[-]	TESTSET  	13 13 -30	; "header"
	206	[-]	GETTABLE 	14 -50	; "Content-Disposition"
	207	[-]	GETTABLE 	15 -51	; "attachment; filename="
	208	[-]	TESTSET  	16 0 -52	; "basename"
	209	[-]	CLOSURE  	17 4
	210	[-]	MOD      	16 2 2
	211	[-]	CONCAT   	15 15 16
	212	[-]	MOD      	13 3 1
	213	[-]	(null)   	13 -8	; string
	214	[-]	TESTSET  	13 13 -19	; "sub"
	215	[-]	CLOSURE  	14 4
	216	[-]	GETTABLE 	15 -20	; 1
	217	[-]	(null)   	16 -8	; string
	218	[-]	TESTSET  	16 16 -21	; "len"
	219	[-]	CLOSURE  	17 5
	220	[-]	MOD      	16 2 0
	221	[-]	MOD      	13 0 2
	222	[-]	NOT      	0 13 5
	223	[-]	FORLOOP  	22	; to 246
	224	[-]	UNM      	13 0	; -
	225	[-]	TESTSET  	13 13 -30	; "header"
	226	[-]	GETTABLE 	14 -53	; "X-Accel-Redirect"
	227	[-]	GETTABLE 	15 -54	; "/download-userdisk/"
	228	[-]	(null)   	16 -55	; pathEncode
	229	[-]	(null)   	17 -8	; string
	230	[-]	TESTSET  	17 17 -19	; "sub"
	231	[-]	CLOSURE  	18 4
	232	[-]	(null)   	19 -8	; string
	233	[-]	TESTSET  	19 19 -21	; "len"
	234	[-]	CLOSURE  	20 5
	235	[-]	MOD      	19 2 2
	236	[-]	VARARG   	19 19 -20	; - 1
	237	[-]	(null)   	20 -8	; string
	238	[-]	TESTSET  	20 20 -21	; "len"
	239	[-]	CLOSURE  	21 4
	240	[-]	MOD      	20 2 0
	241	[-]	MOD      	17 0 0
	242	[-]	MOD      	16 0 2
	243	[-]	CONCAT   	15 15 16
	244	[-]	MOD      	13 3 1
	245	[-]	FORLOOP  	131	; to 377
	246	[-]	(null)   	13 -8	; string
	247	[-]	TESTSET  	13 13 -19	; "sub"
	248	[-]	CLOSURE  	14 4
	249	[-]	GETTABLE 	15 -20	; 1
	250	[-]	(null)   	16 -8	; string
	251	[-]	TESTSET  	16 16 -21	; "len"
	252	[-]	CLOSURE  	17 6
	253	[-]	MOD      	16 2 0
	254	[-]	MOD      	13 0 2
	255	[-]	NOT      	0 13 6
	256	[-]	FORLOOP  	22	; to 279
	257	[-]	UNM      	13 0	; -
	258	[-]	TESTSET  	13 13 -30	; "header"
	259	[-]	GETTABLE 	14 -53	; "X-Accel-Redirect"
	260	[-]	GETTABLE 	15 -56	; "/download-mnt/"
	261	[-]	(null)   	16 -55	; pathEncode
	262	[-]	(null)   	17 -8	; string
	263	[-]	TESTSET  	17 17 -19	; "sub"
	264	[-]	CLOSURE  	18 4
	265	[-]	(null)   	19 -8	; string
	266	[-]	TESTSET  	19 19 -21	; "len"
	267	[-]	CLOSURE  	20 6
	268	[-]	MOD      	19 2 2
	269	[-]	VARARG   	19 19 -20	; - 1
	270	[-]	(null)   	20 -8	; string
	271	[-]	TESTSET  	20 20 -21	; "len"
	272	[-]	CLOSURE  	21 4
	273	[-]	MOD      	20 2 0
	274	[-]	MOD      	17 0 0
	275	[-]	MOD      	16 0 2
	276	[-]	CONCAT   	15 15 16
	277	[-]	MOD      	13 3 1
	278	[-]	FORLOOP  	98	; to 377
	279	[-]	(null)   	13 -8	; string
	280	[-]	TESTSET  	13 13 -19	; "sub"
	281	[-]	CLOSURE  	14 4
	282	[-]	GETTABLE 	15 -20	; 1
	283	[-]	(null)   	16 -8	; string
	284	[-]	TESTSET  	16 16 -21	; "len"
	285	[-]	CLOSURE  	17 7
	286	[-]	MOD      	16 2 0
	287	[-]	MOD      	13 0 2
	288	[-]	NOT      	0 13 7
	289	[-]	FORLOOP  	22	; to 312
	290	[-]	UNM      	13 0	; -
	291	[-]	TESTSET  	13 13 -30	; "header"
	292	[-]	GETTABLE 	14 -53	; "X-Accel-Redirect"
	293	[-]	GETTABLE 	15 -57	; "/download-pridisk/"
	294	[-]	(null)   	16 -55	; pathEncode
	295	[-]	(null)   	17 -8	; string
	296	[-]	TESTSET  	17 17 -19	; "sub"
	297	[-]	CLOSURE  	18 4
	298	[-]	(null)   	19 -8	; string
	299	[-]	TESTSET  	19 19 -21	; "len"
	300	[-]	CLOSURE  	20 7
	301	[-]	MOD      	19 2 2
	302	[-]	VARARG   	19 19 -20	; - 1
	303	[-]	(null)   	20 -8	; string
	304	[-]	TESTSET  	20 20 -21	; "len"
	305	[-]	CLOSURE  	21 4
	306	[-]	MOD      	20 2 0
	307	[-]	MOD      	17 0 0
	308	[-]	MOD      	16 0 2
	309	[-]	CONCAT   	15 15 16
	310	[-]	MOD      	13 3 1
	311	[-]	FORLOOP  	65	; to 377
	312	[-]	(null)   	13 -8	; string
	313	[-]	TESTSET  	13 13 -19	; "sub"
	314	[-]	CLOSURE  	14 4
	315	[-]	GETTABLE 	15 -20	; 1
	316	[-]	(null)   	16 -8	; string
	317	[-]	TESTSET  	16 16 -21	; "len"
	318	[-]	CLOSURE  	17 8
	319	[-]	MOD      	16 2 0
	320	[-]	MOD      	13 0 2
	321	[-]	NOT      	0 13 8
	322	[-]	FORLOOP  	22	; to 345
	323	[-]	UNM      	13 0	; -
	324	[-]	TESTSET  	13 13 -30	; "header"
	325	[-]	GETTABLE 	14 -53	; "X-Accel-Redirect"
	326	[-]	GETTABLE 	15 -58	; "/download-userdisk-appdata/"
	327	[-]	(null)   	16 -55	; pathEncode
	328	[-]	(null)   	17 -8	; string
	329	[-]	TESTSET  	17 17 -19	; "sub"
	330	[-]	CLOSURE  	18 4
	331	[-]	(null)   	19 -8	; string
	332	[-]	TESTSET  	19 19 -21	; "len"
	333	[-]	CLOSURE  	20 8
	334	[-]	MOD      	19 2 2
	335	[-]	VARARG   	19 19 -20	; - 1
	336	[-]	(null)   	20 -8	; string
	337	[-]	TESTSET  	20 20 -21	; "len"
	338	[-]	CLOSURE  	21 4
	339	[-]	MOD      	20 2 0
	340	[-]	MOD      	17 0 0
	341	[-]	MOD      	16 0 2
	342	[-]	CONCAT   	15 15 16
	343	[-]	MOD      	13 3 1
	344	[-]	FORLOOP  	32	; to 377
	345	[-]	(null)   	13 -8	; string
	346	[-]	TESTSET  	13 13 -19	; "sub"
	347	[-]	CLOSURE  	14 4
	348	[-]	GETTABLE 	15 -20	; 1
	349	[-]	(null)   	16 -8	; string
	350	[-]	TESTSET  	16 16 -21	; "len"
	351	[-]	CLOSURE  	17 9
	352	[-]	MOD      	16 2 0
	353	[-]	MOD      	13 0 2
	354	[-]	NOT      	0 13 9
	355	[-]	FORLOOP  	21	; to 377
	356	[-]	UNM      	13 0	; -
	357	[-]	TESTSET  	13 13 -30	; "header"
	358	[-]	GETTABLE 	14 -53	; "X-Accel-Redirect"
	359	[-]	GETTABLE 	15 -59	; "/download-userdisk-thumbnails/"
	360	[-]	(null)   	16 -55	; pathEncode
	361	[-]	(null)   	17 -8	; string
	362	[-]	TESTSET  	17 17 -19	; "sub"
	363	[-]	CLOSURE  	18 4
	364	[-]	(null)   	19 -8	; string
	365	[-]	TESTSET  	19 19 -21	; "len"
	366	[-]	CLOSURE  	20 9
	367	[-]	MOD      	19 2 2
	368	[-]	VARARG   	19 19 -20	; - 1
	369	[-]	(null)   	20 -8	; string
	370	[-]	TESTSET  	20 20 -21	; "len"
	371	[-]	CLOSURE  	21 4
	372	[-]	MOD      	20 2 0
	373	[-]	MOD      	17 0 0
	374	[-]	MOD      	16 0 2
	375	[-]	CONCAT   	15 15 16
	376	[-]	MOD      	13 3 1
	377	[-]	SETUPVAL 	0 1
constants (59) for 0x4000008fa820:
	1	"require"
	2	"nixio.fs"
	3	"luci.http.protocol.mime"
	4	"luci.ltn12"
	5	"xiaoqiang.XQLog"
	6	"formvalue"
	7	"path"
	8	"string"
	9	"isStrNil"
	10	"status"
	11	404
	12	"_"
	13	"no Such file"
	14	"/userdisk/data/"
	15	"/mnt/"
	16	"/userdisk/privacyData/"
	17	"/userdisk/appdata/"
	18	"/userdisk/.thumbnails/"
	19	"sub"
	20	1
	21	"len"
	22	403
	23	"no permission"
	24	"log"
	25	7
	26	"=============path = "
	27	"find"
	28	"/%.%./"
	29	"stat"
	30	"header"
	31	"Accept-Ranges"
	32	"bytes"
	33	"Content-Type"
	34	"to_mime"
	35	"getenv"
	36	"HTTP_RANGE"
	37	206
	38	"gsub"
	39	"bytes="
	40	""
	41	"-"
	42	0
	43	"=============range = "
	44	"bytes "
	45	"size"
	46	"/"
	47	"=============contentRange = "
	48	"Content-Length"
	49	"Content-Range"
	50	"Content-Disposition"
	51	"attachment; filename="
	52	"basename"
	53	"X-Accel-Redirect"
	54	"/download-userdisk/"
	55	"pathEncode"
	56	"/download-mnt/"
	57	"/download-pridisk/"
	58	"/download-userdisk-appdata/"
	59	"/download-userdisk-thumbnails/"
locals (0) for 0x4000008fa820:
upvalues (0) for 0x4000008fa820:

function <?:255,320> (108 instructions, 432 bytes at 0x4000008fa8a0)
0 params, 18 slots, 1 upvalue, 0 locals, 30 constants, 1 function
	1	[-]	(null)   	1 -1	; require
	2	[-]	GETTABLE 	2 -2	; "xiaoqiang.XQLog"
	3	[-]	MOD      	1 2 2
	4	[-]	(null)   	2 -1	; require
	5	[-]	GETTABLE 	3 -3	; "luci.fs"
	6	[-]	MOD      	2 2 2
	7	[-]	GETTABLE 	3 -4	; "/userdisk/upload.tmp"
	8	[-]	TESTSET  	4 2 -5	; "isfile"
	9	[-]	CLOSURE  	5 3
	10	[-]	MOD      	4 2 2
	11	[-]	SETTABLE 	4 0 0
	12	[-]	FORLOOP  	3	; to 16
	13	[-]	TESTSET  	4 2 -6	; "unlink"
	14	[-]	CLOSURE  	5 3
	15	[-]	MOD      	4 2 1
	16	[-]	(null)   	4 4
	17	[-]	UNM      	5 0	; -
	18	[-]	TESTSET  	5 5 -7	; "setfilehandler"
	19	[-]	LOADK    	6 0	; 0x4000008fa920
	20	[-]	CLOSURE  	0 0
	21	[-]	CLOSURE  	0 3
	22	[-]	CLOSURE  	0 4
	23	[-]	MOD      	5 2 1
	24	[-]	UNM      	5 0	; -
	25	[-]	TESTSET  	5 5 -8	; "formvalue"
	26	[-]	GETTABLE 	6 -9	; "target"
	27	[-]	MOD      	5 2 2
	28	[-]	(null)   	6 -10	; string
	29	[-]	TESTSET  	6 6 -11	; "match"
	30	[-]	CLOSURE  	7 5
	31	[-]	GETTABLE 	8 -12	; "/$"
	32	[-]	MOD      	6 3 2
	33	[-]	NOT      	0 6 -13	; - nil
	34	[-]	FORLOOP  	3	; to 38
	35	[-]	CLOSURE  	6 5
	36	[-]	GETTABLE 	7 -14	; "/"
	37	[-]	CONCAT   	5 6 7
	38	[-]	TESTSET  	6 2 -15	; "mkdir"
	39	[-]	CLOSURE  	7 5
	40	[-]	LT       	8 1 0
	41	[-]	MOD      	6 3 1
	42	[-]	CLOSURE  	6 4
	43	[-]	TESTSET  	7 2 -5	; "isfile"
	44	[-]	CLOSURE  	8 5
	45	[-]	CLOSURE  	9 6
	46	[-]	CONCAT   	8 8 9
	47	[-]	MOD      	7 2 2
	48	[-]	SETTABLE 	7 0 0
	49	[-]	FORLOOP  	39	; to 89
	50	[-]	CLOSURE  	7 6
	51	[-]	DIV      	8 7 -11	; "match"
	52	[-]	GETTABLE 	10 -16	; ".+()%.%w+$"
	53	[-]	MOD      	8 3 2
	54	[-]	SETTABLE 	8 0 0
	55	[-]	FORLOOP  	5	; to 61
	56	[-]	DIV      	9 7 -17	; "sub"
	57	[-]	GETTABLE 	11 -18	; 1
	58	[-]	ADD      	12 8 -18	; - 1
	59	[-]	MOD      	9 4 2
	60	[-]	CLOSURE  	7 9
	61	[-]	DIV      	9 6 -11	; "match"
	62	[-]	GETTABLE 	11 -19	; ".+%.(%w+)$"
	63	[-]	MOD      	9 3 2
	64	[-]	GETTABLE 	10 -18	; 1
	65	[-]	GETTABLE 	11 -20	; 100
	66	[-]	GETTABLE 	12 -18	; 1
	67	[-]	SELF     	10 20	; to 88
	68	[-]	CLOSURE  	14 7
	69	[-]	GETTABLE 	15 -21	; "("
	70	[-]	CLOSURE  	16 13
	71	[-]	GETTABLE 	17 -22	; ")"
	72	[-]	CONCAT   	14 14 17
	73	[-]	SETTABLE 	9 0 0
	74	[-]	FORLOOP  	4	; to 79
	75	[-]	CLOSURE  	15 14
	76	[-]	GETTABLE 	16 -23	; "."
	77	[-]	CLOSURE  	17 9
	78	[-]	CONCAT   	14 15 17
	79	[-]	TESTSET  	15 2 -5	; "isfile"
	80	[-]	CLOSURE  	16 5
	81	[-]	CLOSURE  	17 14
	82	[-]	CONCAT   	16 16 17
	83	[-]	MOD      	15 2 2
	84	[-]	SETTABLE 	15 0 1
	85	[-]	FORLOOP  	2	; to 88
	86	[-]	CLOSURE  	6 14
	87	[-]	FORLOOP  	1	; to 89
	88	[-]	TEST     	10 -21	; to 68
	89	[-]	CLOSURE  	7 5
	90	[-]	CLOSURE  	8 6
	91	[-]	CONCAT   	7 7 8
	92	[-]	TESTSET  	8 1 -24	; "log"
	93	[-]	GETTABLE 	9 -25	; 7
	94	[-]	GETTABLE 	10 -26	; "dest="
	95	[-]	CLOSURE  	11 7
	96	[-]	CONCAT   	10 10 11
	97	[-]	MOD      	8 3 1
	98	[-]	TESTSET  	8 2 -27	; "rename"
	99	[-]	CLOSURE  	9 3
	100	[-]	CLOSURE  	10 7
	101	[-]	MOD      	8 3 1
	102	[-]	CALL     	8 0 0
	103	[-]	POW      	8 -28 -29	; "code" 0
	104	[-]	UNM      	9 0	; -
	105	[-]	TESTSET  	9 9 -30	; "write_json"
	106	[-]	CLOSURE  	10 8
	107	[-]	MOD      	9 2 1
	108	[-]	SETUPVAL 	0 1
constants (30) for 0x4000008fa8a0:
	1	"require"
	2	"xiaoqiang.XQLog"
	3	"luci.fs"
	4	"/userdisk/upload.tmp"
	5	"isfile"
	6	"unlink"
	7	"setfilehandler"
	8	"formvalue"
	9	"target"
	10	"string"
	11	"match"
	12	"/$"
	13	nil
	14	"/"
	15	"mkdir"
	16	".+()%.%w+$"
	17	"sub"
	18	1
	19	".+%.(%w+)$"
	20	100
	21	"("
	22	")"
	23	"."
	24	"log"
	25	7
	26	"dest="
	27	"rename"
	28	"code"
	29	0
	30	"write_json"
locals (0) for 0x4000008fa8a0:
upvalues (0) for 0x4000008fa8a0:

function <?:265,284> (49 instructions, 196 bytes at 0x4000008fa920)
3 params, 7 slots, 3 upvalues, 0 locals, 14 constants, 1 function
	1	[-]	UNM      	3 0	; -
	2	[-]	SETTABLE 	3 0 1
	3	[-]	FORLOOP  	34	; to 38
	4	[-]	SETTABLE 	0 0 0
	5	[-]	FORLOOP  	32	; to 38
	6	[-]	TESTSET  	3 0 -1	; "name"
	7	[-]	NOT      	0 3 -2	; - "file"
	8	[-]	FORLOOP  	29	; to 38
	9	[-]	(null)   	3 -3	; io
	10	[-]	TESTSET  	3 3 -4	; "open"
	11	[-]	UNM      	4 1	; -
	12	[-]	GETTABLE 	5 -5	; "w"
	13	[-]	MOD      	3 3 2
	14	[-]	(null)   	3 0	; -
	15	[-]	TESTSET  	3 0 -2	; "file"
	16	[-]	(null)   	3 2	; -
	17	[-]	(null)   	3 -6	; string
	18	[-]	TESTSET  	3 3 -7	; "gsub"
	19	[-]	UNM      	4 2	; -
	20	[-]	GETTABLE 	5 -8	; "+"
	21	[-]	GETTABLE 	6 -9	; " "
	22	[-]	MOD      	3 4 2
	23	[-]	(null)   	3 2	; -
	24	[-]	(null)   	3 -6	; string
	25	[-]	TESTSET  	3 3 -7	; "gsub"
	26	[-]	UNM      	4 2	; -
	27	[-]	GETTABLE 	5 -10	; "%%(%x%x)"
	28	[-]	LOADK    	6 0	; 0x4000008fa9a0
	29	[-]	MOD      	3 4 2
	30	[-]	(null)   	3 2	; -
	31	[-]	UNM      	3 2	; -
	32	[-]	TESTSET  	3 3 -7	; "gsub"
	33	[-]	UNM      	4 2	; -
	34	[-]	GETTABLE 	5 -11	; "\r\n"
	35	[-]	GETTABLE 	6 -12	; "\n"
	36	[-]	MOD      	3 4 2
	37	[-]	(null)   	3 2	; -
	38	[-]	SETTABLE 	1 0 0
	39	[-]	FORLOOP  	4	; to 44
	40	[-]	UNM      	3 0	; -
	41	[-]	DIV      	3 3 -13	; "write"
	42	[-]	CLOSURE  	5 1
	43	[-]	MOD      	3 3 1
	44	[-]	SETTABLE 	2 0 0
	45	[-]	FORLOOP  	3	; to 49
	46	[-]	UNM      	3 0	; -
	47	[-]	DIV      	3 3 -14	; "close"
	48	[-]	MOD      	3 2 1
	49	[-]	SETUPVAL 	0 1
constants (14) for 0x4000008fa920:
	1	"name"
	2	"file"
	3	"io"
	4	"open"
	5	"w"
	6	"string"
	7	"gsub"
	8	"+"
	9	" "
	10	"%%(%x%x)"
	11	"\r\n"
	12	"\n"
	13	"write"
	14	"close"
locals (0) for 0x4000008fa920:
upvalues (0) for 0x4000008fa920:

function <?:272,274> (9 instructions, 36 bytes at 0x4000008fa9a0)
1 param, 5 slots, 0 upvalues, 0 locals, 4 constants, 0 functions
	1	[-]	(null)   	1 -1	; string
	2	[-]	TESTSET  	1 1 -2	; "char"
	3	[-]	(null)   	2 -3	; tonumber
	4	[-]	CLOSURE  	3 0
	5	[-]	GETTABLE 	4 -4	; 16
	6	[-]	MOD      	2 3 0
	7	[-]	SUB      	1 0 0
	8	[-]	SETUPVAL 	1 0
	9	[-]	SETUPVAL 	0 1
constants (4) for 0x4000008fa9a0:
	1	"string"
	2	"char"
	3	"tonumber"
	4	16
locals (0) for 0x4000008fa9a0:
upvalues (0) for 0x4000008fa9a0:

function <?:322,347> (86 instructions, 344 bytes at 0x4000008faa20)
0 params, 15 slots, 2 upvalues, 0 locals, 38 constants, 0 functions
	1	[-]	(null)   	0 -1	; require
	2	[-]	GETTABLE 	1 -2	; "luci.util"
	3	[-]	MOD      	0 2 2
	4	[-]	(null)   	1 -1	; require
	5	[-]	GETTABLE 	2 -3	; "nixio.fs"
	6	[-]	MOD      	1 2 2
	7	[-]	(null)   	2 -1	; require
	8	[-]	GETTABLE 	3 -4	; "luci.http.protocol.mime"
	9	[-]	MOD      	2 2 2
	10	[-]	(null)   	3 -1	; require
	11	[-]	GETTABLE 	4 -5	; "luci.ltn12"
	12	[-]	MOD      	3 2 2
	13	[-]	(null)   	4 -1	; require
	14	[-]	GETTABLE 	5 -6	; "xiaoqiang.XQLog"
	15	[-]	MOD      	4 2 2
	16	[-]	UNM      	5 0	; -
	17	[-]	TESTSET  	5 5 -7	; "formvalue"
	18	[-]	GETTABLE 	6 -8	; "filePath"
	19	[-]	MOD      	5 2 2
	20	[-]	TESTSET  	6 4 -9	; "log"
	21	[-]	GETTABLE 	7 -10	; 7
	22	[-]	GETTABLE 	8 -11	; "realPath = "
	23	[-]	CLOSURE  	9 5
	24	[-]	MOD      	6 4 1
	25	[-]	NOT      	0 5 -12	; - nil
	26	[-]	FORLOOP  	8	; to 35
	27	[-]	UNM      	6 0	; -
	28	[-]	TESTSET  	6 6 -13	; "status"
	29	[-]	GETTABLE 	7 -14	; 404
	30	[-]	(null)   	8 -15	; _
	31	[-]	GETTABLE 	9 -16	; "no Such file"
	32	[-]	MOD      	8 2 0
	33	[-]	MOD      	6 0 1
	34	[-]	SETUPVAL 	0 1
	35	[-]	GETTABLE 	6 -17	; "{\"api\":10, \"files\":[\""
	36	[-]	CLOSURE  	7 5
	37	[-]	GETTABLE 	8 -18	; "\"]}"
	38	[-]	CONCAT   	6 6 8
	39	[-]	UNM      	7 1	; -
	40	[-]	TESTSET  	7 7 -19	; "thrift_tunnel_to_datacenter"
	41	[-]	CLOSURE  	8 6
	42	[-]	MOD      	7 2 2
	43	[-]	SETTABLE 	7 0 0
	44	[-]	FORLOOP  	34	; to 79
	45	[-]	TESTSET  	8 7 -20	; "code"
	46	[-]	NOT      	0 8 -21	; - 0
	47	[-]	FORLOOP  	31	; to 79
	48	[-]	TESTSET  	8 7 -22	; "thumbnails"
	49	[-]	TESTSET  	8 8 -23	; 1
	50	[-]	TESTSET  	9 1 -24	; "stat"
	51	[-]	CLOSURE  	10 8
	52	[-]	MOD      	9 2 2
	53	[-]	UNM      	10 0	; -
	54	[-]	TESTSET  	10 10 -25	; "header"
	55	[-]	GETTABLE 	11 -26	; "Content-Type"
	56	[-]	TESTSET  	12 2 -27	; "to_mime"
	57	[-]	CLOSURE  	13 8
	58	[-]	MOD      	12 2 0
	59	[-]	MOD      	10 0 1
	60	[-]	UNM      	10 0	; -
	61	[-]	TESTSET  	10 10 -25	; "header"
	62	[-]	GETTABLE 	11 -28	; "Content-Length"
	63	[-]	TESTSET  	12 9 -29	; "size"
	64	[-]	MOD      	10 3 1
	65	[-]	TESTSET  	10 3 -30	; "pump"
	66	[-]	TESTSET  	10 10 -31	; "all"
	67	[-]	TESTSET  	11 3 -32	; "source"
	68	[-]	TESTSET  	11 11 -33	; "file"
	69	[-]	(null)   	12 -34	; io
	70	[-]	TESTSET  	12 12 -35	; "open"
	71	[-]	CLOSURE  	13 8
	72	[-]	GETTABLE 	14 -36	; "r"
	73	[-]	MOD      	12 3 0
	74	[-]	MOD      	11 0 2
	75	[-]	UNM      	12 0	; -
	76	[-]	TESTSET  	12 12 -37	; "write"
	77	[-]	MOD      	10 3 1
	78	[-]	FORLOOP  	7	; to 86
	79	[-]	UNM      	8 0	; -
	80	[-]	TESTSET  	8 8 -13	; "status"
	81	[-]	GETTABLE 	9 -14	; 404
	82	[-]	(null)   	10 -15	; _
	83	[-]	GETTABLE 	11 -38	; "no Such thumb file"
	84	[-]	MOD      	10 2 0
	85	[-]	MOD      	8 0 1
	86	[-]	SETUPVAL 	0 1
constants (38) for 0x4000008faa20:
	1	"require"
	2	"luci.util"
	3	"nixio.fs"
	4	"luci.http.protocol.mime"
	5	"luci.ltn12"
	6	"xiaoqiang.XQLog"
	7	"formvalue"
	8	"filePath"
	9	"log"
	10	7
	11	"realPath = "
	12	nil
	13	"status"
	14	404
	15	"_"
	16	"no Such file"
	17	"{\"api\":10, \"files\":[\""
	18	"\"]}"
	19	"thrift_tunnel_to_datacenter"
	20	"code"
	21	0
	22	"thumbnails"
	23	1
	24	"stat"
	25	"header"
	26	"Content-Type"
	27	"to_mime"
	28	"Content-Length"
	29	"size"
	30	"pump"
	31	"all"
	32	"source"
	33	"file"
	34	"io"
	35	"open"
	36	"r"
	37	"write"
	38	"no Such thumb file"
locals (0) for 0x4000008faa20:
upvalues (0) for 0x4000008faa20:

function <?:349,367> (30 instructions, 120 bytes at 0x4000008faaa0)
0 params, 6 slots, 2 upvalues, 0 locals, 10 constants, 0 functions
	1	[-]	(null)   	0 -1	; require
	2	[-]	GETTABLE 	1 -2	; "nixio.fs"
	3	[-]	MOD      	0 2 2
	4	[-]	LT       	1 1 0
	5	[-]	UNM      	2 0	; -
	6	[-]	TESTSET  	2 2 -3	; "formvalue"
	7	[-]	GETTABLE 	3 -4	; "filePath"
	8	[-]	MOD      	2 2 2
	9	[-]	UNM      	3 1	; -
	10	[-]	TESTSET  	3 3 -5	; "isStrNil"
	11	[-]	CLOSURE  	4 2
	12	[-]	MOD      	3 2 2
	13	[-]	SETTABLE 	3 0 0
	14	[-]	FORLOOP  	2	; to 17
	15	[-]	LT       	1 0 0
	16	[-]	FORLOOP  	6	; to 23
	17	[-]	TESTSET  	3 0 -6	; "stat"
	18	[-]	CLOSURE  	4 2
	19	[-]	MOD      	3 2 2
	20	[-]	SETTABLE 	3 0 1
	21	[-]	FORLOOP  	1	; to 23
	22	[-]	LT       	1 0 0
	23	[-]	CALL     	3 0 0
	24	[-]	POW      	3 -7 -8	; "code" 0
	25	[-]	POW      	3 -9 1	; "exist" -
	26	[-]	UNM      	4 0	; -
	27	[-]	TESTSET  	4 4 -10	; "write_json"
	28	[-]	CLOSURE  	5 3
	29	[-]	MOD      	4 2 1
	30	[-]	SETUPVAL 	0 1
constants (10) for 0x4000008faaa0:
	1	"require"
	2	"nixio.fs"
	3	"formvalue"
	4	"filePath"
	5	"isStrNil"
	6	"stat"
	7	"code"
	8	0
	9	"exist"
	10	"write_json"
locals (0) for 0x4000008faaa0:
upvalues (0) for 0x4000008faaa0:

function <?:369,408> (92 instructions, 368 bytes at 0x4000008fe800)
0 params, 11 slots, 4 upvalues, 0 locals, 26 constants, 0 functions
	1	[-]	(null)   	0 -1	; require
	2	[-]	GETTABLE 	1 -2	; "luci.util"
	3	[-]	MOD      	0 2 2
	4	[-]	(null)   	1 -1	; require
	5	[-]	GETTABLE 	2 -3	; "xiaoqiang.XQLog"
	6	[-]	MOD      	1 2 2
	7	[-]	GETTABLE 	2 -4	; 0
	8	[-]	CALL     	3 0 0
	9	[-]	UNM      	4 0	; -
	10	[-]	TESTSET  	4 4 -5	; "formvalue"
	11	[-]	GETTABLE 	5 -6	; "pluginID"
	12	[-]	MOD      	4 2 2
	13	[-]	UNM      	5 0	; -
	14	[-]	TESTSET  	5 5 -5	; "formvalue"
	15	[-]	GETTABLE 	6 -7	; "capability"
	16	[-]	MOD      	5 2 2
	17	[-]	(null)   	6 -8	; tonumber
	18	[-]	UNM      	7 0	; -
	19	[-]	TESTSET  	7 7 -5	; "formvalue"
	20	[-]	GETTABLE 	8 -9	; "open"
	21	[-]	MOD      	7 2 2
	22	[-]	SETTABLE 	7 0 1
	23	[-]	FORLOOP  	1	; to 25
	24	[-]	GETTABLE 	7 -4	; 0
	25	[-]	MOD      	6 2 2
	26	[-]	TESTSET  	7 1 -10	; "check"
	27	[-]	GETTABLE 	8 -4	; 0
	28	[-]	TESTSET  	9 1 -11	; "KEY_FUNC_PLUGIN"
	29	[-]	GETTABLE 	10 -12	; 1
	30	[-]	MOD      	7 4 1
	31	[-]	SETTABLE 	6 0 0
	32	[-]	FORLOOP  	30	; to 63
	33	[-]	NOT      	0 6 -12	; - 1
	34	[-]	FORLOOP  	28	; to 63
	35	[-]	SETTABLE 	4 0 0
	36	[-]	FORLOOP  	24	; to 61
	37	[-]	SETTABLE 	5 0 0
	38	[-]	FORLOOP  	22	; to 61
	39	[-]	CALL     	7 0 3
	40	[-]	POW      	7 -13 -14	; "api" 611
	41	[-]	POW      	7 -6 4	; "pluginID" -
	42	[-]	TESTSET  	8 0 -15	; "split"
	43	[-]	CLOSURE  	9 5
	44	[-]	GETTABLE 	10 -16	; ","
	45	[-]	MOD      	8 3 2
	46	[-]	POW      	7 -7 8	; "capability" -
	47	[-]	UNM      	8 1	; -
	48	[-]	TESTSET  	8 8 -17	; "thrift_tunnel_to_datacenter"
	49	[-]	UNM      	9 2	; -
	50	[-]	TESTSET  	9 9 -18	; "encode"
	51	[-]	CLOSURE  	10 7
	52	[-]	MOD      	9 2 0
	53	[-]	MOD      	8 0 2
	54	[-]	SETTABLE 	8 0 0
	55	[-]	FORLOOP  	24	; to 80
	56	[-]	TESTSET  	9 8 -19	; "code"
	57	[-]	LEN      	1 9 -4
	58	[-]	FORLOOP  	21	; to 80
	59	[-]	GETTABLE 	2 -20	; 1595
	60	[-]	FORLOOP  	19	; to 80
	61	[-]	GETTABLE 	2 -21	; 1537
	62	[-]	FORLOOP  	17	; to 80
	63	[-]	CALL     	7 0 1
	64	[-]	POW      	7 -13 -22	; "api" 613
	65	[-]	UNM      	8 1	; -
	66	[-]	TESTSET  	8 8 -17	; "thrift_tunnel_to_datacenter"
	67	[-]	UNM      	9 2	; -
	68	[-]	TESTSET  	9 9 -18	; "encode"
	69	[-]	CLOSURE  	10 7
	70	[-]	MOD      	9 2 0
	71	[-]	MOD      	8 0 2
	72	[-]	SETTABLE 	8 0 0
	73	[-]	FORLOOP  	5	; to 79
	74	[-]	TESTSET  	9 8 -19	; "code"
	75	[-]	NOT      	0 9 -4	; - 0
	76	[-]	FORLOOP  	2	; to 79
	77	[-]	GETTABLE 	2 -4	; 0
	78	[-]	FORLOOP  	1	; to 80
	79	[-]	GETTABLE 	2 -23	; 1601
	80	[-]	LEN      	1 2 -4
	81	[-]	FORLOOP  	5	; to 87
	82	[-]	UNM      	7 3	; -
	83	[-]	TESTSET  	7 7 -25	; "getErrorMessage"
	84	[-]	CLOSURE  	8 2
	85	[-]	MOD      	7 2 2
	86	[-]	POW      	3 -24 7	; "msg" -
	87	[-]	POW      	3 -19 2	; "code" -
	88	[-]	UNM      	7 0	; -
	89	[-]	TESTSET  	7 7 -26	; "write_json"
	90	[-]	CLOSURE  	8 3
	91	[-]	MOD      	7 2 1
	92	[-]	SETUPVAL 	0 1
constants (26) for 0x4000008fe800:
	1	"require"
	2	"luci.util"
	3	"xiaoqiang.XQLog"
	4	0
	5	"formvalue"
	6	"pluginID"
	7	"capability"
	8	"tonumber"
	9	"open"
	10	"check"
	11	"KEY_FUNC_PLUGIN"
	12	1
	13	"api"
	14	611
	15	"split"
	16	","
	17	"thrift_tunnel_to_datacenter"
	18	"encode"
	19	"code"
	20	1595
	21	1537
	22	613
	23	1601
	24	"msg"
	25	"getErrorMessage"
	26	"write_json"
locals (0) for 0x4000008fe800:
upvalues (0) for 0x4000008fe800:

function <?:410,443> (76 instructions, 304 bytes at 0x4000008fe880)
0 params, 17 slots, 3 upvalues, 0 locals, 20 constants, 0 functions
	1	[-]	GETTABLE 	0 -1	; 0
	2	[-]	CALL     	1 0 0
	3	[-]	UNM      	2 0	; -
	4	[-]	TESTSET  	2 2 -2	; "thrift_tunnel_to_datacenter"
	5	[-]	GETTABLE 	3 -3	; "{\"api\":612}"
	6	[-]	MOD      	2 2 2
	7	[-]	UNM      	3 0	; -
	8	[-]	TESTSET  	3 3 -2	; "thrift_tunnel_to_datacenter"
	9	[-]	GETTABLE 	4 -4	; "{\"api\":621}"
	10	[-]	MOD      	3 2 2
	11	[-]	SETTABLE 	2 0 0
	12	[-]	FORLOOP  	50	; to 63
	13	[-]	TESTSET  	4 2 -5	; "code"
	14	[-]	NOT      	0 4 -1	; - 0
	15	[-]	FORLOOP  	47	; to 63
	16	[-]	SETTABLE 	3 0 0
	17	[-]	FORLOOP  	45	; to 63
	18	[-]	TESTSET  	4 2 -5	; "code"
	19	[-]	NOT      	0 4 -1	; - 0
	20	[-]	FORLOOP  	42	; to 63
	21	[-]	CALL     	4 0 0
	22	[-]	TESTSET  	5 2 -7	; "status"
	23	[-]	NOT      	0 5 -8	; - 1
	24	[-]	FORLOOP  	3	; to 28
	25	[-]	GETTABLE 	5 -8	; 1
	26	[-]	SETTABLE 	5 0 1
	27	[-]	FORLOOP  	1	; to 29
	28	[-]	GETTABLE 	5 -1	; 0
	29	[-]	POW      	1 -6 5	; "enable" -
	30	[-]	CALL     	5 0 0
	31	[-]	TESTSET  	6 1 -6	; "enable"
	32	[-]	NOT      	0 6 -8	; - 1
	33	[-]	FORLOOP  	4	; to 38
	34	[-]	TESTSET  	6 2 -9	; "plugin_ssh_status"
	35	[-]	TESTSET  	7 6 -10	; "pluginID"
	36	[-]	POW      	1 -10 7	; "pluginID" -
	37	[-]	TESTSET  	5 6 -11	; "capability"
	38	[-]	(null)   	6 -12	; ipairs
	39	[-]	TESTSET  	7 3 -13	; "list"
	40	[-]	MOD      	6 2 4
	41	[-]	FORLOOP  	17	; to 59
	42	[-]	POW      	10 -6 -1	; "enable" 0
	43	[-]	(null)   	11 -12	; ipairs
	44	[-]	CLOSURE  	12 5
	45	[-]	MOD      	11 2 4
	46	[-]	FORLOOP  	5	; to 52
	47	[-]	TESTSET  	16 10 -14	; "key"
	48	[-]	NOT      	0 16 15
	49	[-]	FORLOOP  	2	; to 52
	50	[-]	POW      	10 -6 -8	; "enable" 1
	51	[-]	FORLOOP  	2	; to 54
	52	[-]	NEWTABLE 	11 2
	53	[-]	FORLOOP  	-7	; to 47
	54	[-]	(null)   	11 -15	; table
	55	[-]	TESTSET  	11 11 -16	; "insert"
	56	[-]	CLOSURE  	12 4
	57	[-]	CLOSURE  	13 10
	58	[-]	MOD      	11 3 1
	59	[-]	NEWTABLE 	6 2
	60	[-]	FORLOOP  	-19	; to 42
	61	[-]	POW      	1 -11 4	; "capability" -
	62	[-]	FORLOOP  	1	; to 64
	63	[-]	GETTABLE 	0 -17	; 1600
	64	[-]	LEN      	1 0 -1
	65	[-]	FORLOOP  	5	; to 71
	66	[-]	UNM      	4 1	; -
	67	[-]	TESTSET  	4 4 -19	; "getErrorMessage"
	68	[-]	CLOSURE  	5 0
	69	[-]	MOD      	4 2 2
	70	[-]	POW      	1 -18 4	; "msg" -
	71	[-]	POW      	1 -5 0	; "code" -
	72	[-]	UNM      	4 2	; -
	73	[-]	TESTSET  	4 4 -20	; "write_json"
	74	[-]	CLOSURE  	5 1
	75	[-]	MOD      	4 2 1
	76	[-]	SETUPVAL 	0 1
constants (20) for 0x4000008fe880:
	1	0
	2	"thrift_tunnel_to_datacenter"
	3	"{\"api\":612}"
	4	"{\"api\":621}"
	5	"code"
	6	"enable"
	7	"status"
	8	1
	9	"plugin_ssh_status"
	10	"pluginID"
	11	"capability"
	12	"ipairs"
	13	"list"
	14	"key"
	15	"table"
	16	"insert"
	17	1600
	18	"msg"
	19	"getErrorMessage"
	20	"write_json"
locals (0) for 0x4000008fe880:
upvalues (0) for 0x4000008fe880:
