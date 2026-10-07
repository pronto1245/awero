export type MissionType = "MATH" | "STEPS" | "QR" | "PHOTO" | "MIXED";
export type Difficulty = "EASY" | "MEDIUM" | "HARD";
export type WakeResult = "COMPLETED" | "FAILED" | "EMERGENCY_STOP";
export interface WakeSessionSummary { sessionId:string; alarmId:string; missionType:MissionType; result:WakeResult; completionTimeSeconds:number|null; snoozeCount:number; fallbackUsed:boolean; }
export interface AIRecommendation { mission_type:MissionType; difficulty:Difficulty; steps_target?:number; reason_code:string; confidence:number; }