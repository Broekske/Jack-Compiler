public class JackSymbol {

	public final String name;
    public final String type;// int, char, bool
	public final String kind;//FIELD, STATIC, VAR, ARG
	public final int index;

    public JackSymbol(String name, String type, String kind, int index){
        this.name = name;
        this.type = type;
		this.kind = kind;
		this.index = index;
    }
}
