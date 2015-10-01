package bcu.nopol.control;

public enum IfUsage {

	NONE, THEN, ELSE, BOTH;
	
	public static IfUsage fromBool(Boolean b){
		if(b==null)return NONE;
		if(b)
			return THEN;
		return ELSE;
	}
}
