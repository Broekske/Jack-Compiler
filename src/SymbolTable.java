import java.util.Hashtable;

public class SymbolTable {

	Hashtable<String, JackSymbol> classSymbols;
	Hashtable<String, JackSymbol> subroutineSymbols;

	private int argumentIndex = 0;
	private int varIndex = 0;
	private int fieldIndex = 0;
	private int staticIndex = 0;

	public SymbolTable(){
	
		subroutineSymbols = new Hashtable<String, JackSymbol>();
		classSymbols = new Hashtable<String, JackSymbol>();
	}

	public void define(String name, String type, String kind){
		int index = 0;
		Hashtable<String, JackSymbol> symbols = null;
		switch(kind){
			case "VAR": index = varIndex++; symbols = subroutineSymbols; break;
			case "ARG": index = argumentIndex++; symbols = subroutineSymbols; break;
			case "FIELD": index = fieldIndex++; symbols = classSymbols; break;
			case "STATIC": index = staticIndex++; symbols = classSymbols; break;
		}
		JackSymbol s = new JackSymbol(name,type,kind,index);
		symbols.put(s.name, s);
	}

	public void startSubroutine(){
		subroutineSymbols = new Hashtable<String, JackSymbol>();
		argumentIndex = 0;
		varIndex = 0;
	}

	public int varCount(String kind){
		switch(kind){
			case "VAR": return varIndex;
			case "ARG": return argumentIndex;
			case "FIELD": return fieldIndex;
			case "STATIC": return staticIndex;
		}
		return -1;
	}

	public String kindOf(String name){
		JackSymbol symbol = getJackSymbol(name);
		if(symbol != null) return symbol.kind;
		else return "NONE";
	}

	public String typeOf(String name){
		JackSymbol symbol = getJackSymbol(name);
		if(symbol != null) return symbol.type;
		else return null;

	}

	public int indexOf(String name){
		JackSymbol symbol = getJackSymbol(name);
		return symbol.index;

	}

	private JackSymbol getJackSymbol(String name){
		JackSymbol symbol = subroutineSymbols.get(name);
		if(symbol == null){
		symbol = classSymbols.get(name);
		}
		return symbol;
	}

}
