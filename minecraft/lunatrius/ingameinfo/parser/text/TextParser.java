package lunatrius.ingameinfo.parser.text;

import cpw.mods.fml.common.FMLCommonHandler;
import lunatrius.ingameinfo.InGameInfoCore;
import lunatrius.ingameinfo.InGameInfoXML;
import lunatrius.ingameinfo.Value;
import lunatrius.ingameinfo.parser.IParser;
import lunatrius.ingameinfo.parser.ParserUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static lunatrius.ingameinfo.Value.ValueType;
import static lunatrius.ingameinfo.parser.text.Token.TokenType;

public class TextParser implements IParser {
	private final Tokenizer tokenizer;
	private Token token;
	private int level = 0;
	private String position = "topleft";

	public TextParser() {
		this.tokenizer = new Tokenizer();
	}

	private Token nextToken() {
		this.token = this.tokenizer.nextToken();
		return this.token;
	}

	@Override
	public boolean load(File file) {
		if (file.exists()) {
			try {
				FileReader fileReader = new FileReader(file);
				BufferedReader reader = new BufferedReader(fileReader);
				String line, content = "";

				while ((line = reader.readLine()) != null) {
					content += line + "\n";
				}

				reader.close();
				fileReader.close();

				tokenizer.tokenize(content);
			} catch (Exception e) {
		InGameInfoXML.LOGGER.log(Level.SEVERE, "Could not read text configuration file!", e);
			}
		}

		return false;
	}

	@Override
	public boolean parse(Map<String, List<List<Value>>> format) {
		boolean expr;

		try {
			nextToken();
			expr = alignments(format) && this.token.isEof();
		} catch (Exception e) {
			expr = false;
			e.printStackTrace();
		}

		return expr;
	}

	private boolean alignments(Map<String, List<List<Value>>> format) {
		return alignment(format) && alignmentsTail(format);
	}

	private boolean alignmentsTail(Map<String, List<List<Value>>> format) {
		if (alignment(format)) {
			alignmentsTail(format);
		}

		return true;
	}

	private boolean alignment(Map<String, List<List<Value>>> format) {
		boolean expr;
		List<List<Value>> lines = format.get(this.position);

		if (lines == null) {
			lines = new ArrayList<List<Value>>();
		}

		expr = lines(lines);

		if (expr) {
			format.put(this.position, lines);
		}

		return expr;
	}

	private boolean lines(List<List<Value>> lines) {
		return line(lines) && linesTail(lines);
	}

	private boolean linesTail(List<List<Value>> lines) {
		if (line(lines)) {
			linesTail(lines);
		}

		return true;
	}

	private boolean line(List<List<Value>> lines) {
		boolean expr;
		List<Value> values = new ArrayList<Value>();

		expr = values(values);

		if (this.token.getType().equals(TokenType.NEWLINE)) {
			nextToken();
		}

		if (expr) {
			lines.add(values);
		}

		return expr;
	}

	private boolean values(List<Value> values) {
		return value(values) && valuesTail(values);
	}

	private boolean valuesTail(List<Value> values) {
		if (value(values)) {
			valuesTail(values);
		}

		return true;
	}

	private boolean value(List<Value> values) {
		boolean expr;

		if (this.token.getType().equals(TokenType.STRING)) {
			expr = string(values, this.token.getLexem());
			nextToken();
		} else if (this.token.getType().equals(TokenType.FUNC_HEAD)) {
			nextToken();
			expr = function(values, this.token.getLexem());
		} else if (this.level == 0 && TokenType.EXCEPTIONS.contains(this.token.getType())) {
			expr = string(values, this.token.getLexem());
			nextToken();
		} else {
			expr = false;
		}

		return expr;
	}

	private boolean string(List<Value> values, String lexem) {
		values.add(new Value(ValueType.STR, lexem.replaceAll("\\$(?=[0-9a-fk-or])", "\u00a7")));
		return true;
	}

	private boolean function(List<Value> values, String lexem) {
		boolean expr;

		this.level++;

		ValueType type = ValueType.fromString(lexem);
		Value value = ((type == ValueType.NONE) ? new Value(ValueType.VAR, lexem) : new Value(type, ""));

		if (this.token.getType().equals(TokenType.STRING)) {
			nextToken();

			expr = argumentGroupA(value);

			if (this.token.getType().equals(TokenType.FUNC_TAIL)) {
				nextToken();
			} else {
				expr = false;
			}
		} else {
			expr = false;
		}

		String position = ParserUtils.getPosition(lexem);
		if (position != null) {
			this.position = position;
		} else if (expr) {
			values.add(value);
		}

		this.level--;

		return expr;
	}

	private boolean argumentGroupA(Value value) {
		boolean expr;

		if (this.token.getType().equals(TokenType.ARGS_HEAD)) {
			nextToken();

			expr = argumentsA(value);

			expr &= argumentGroupB(value);

			if (this.token.getType().equals(TokenType.ARGS_TAIL)) {
				nextToken();
			} else {
				expr = false;
			}
		} else {
			expr = true;
		}

		return expr;
	}

	private boolean argumentsA(Value value) {
		return argument(value) && argumentsATail(value);
	}

	private boolean argumentsATail(Value value) {
		if (this.token.getType().equals(TokenType.ARGS_SEPARATOR)) {
			nextToken();

			if (argument(value)) {
				argumentsATail(value);
			}
		}

		return true;
	}

	private boolean argument(Value value) {
		boolean expr;

		if (this.token.getType().equals(TokenType.STRING)) {
			expr = string(value.values, this.token.getLexem());
			nextToken();
		} else if (this.token.getType().equals(TokenType.FUNC_HEAD)) {
			nextToken();
			expr = function(value.values, this.token.getLexem());
		} else {
			expr = string(value.values, "");
		}

		return expr;
	}

	private boolean argumentGroupB(Value value) {
		boolean expr;

		if (this.token.getType().equals(TokenType.ARGS_HEAD)) {
			nextToken();

			expr = argumentsB(value);

			if (this.token.getType().equals(TokenType.ARGS_TAIL)) {
				nextToken();
			} else {
				expr = false;
			}
		} else {
			expr = true;
		}

		return expr;
	}

	private boolean argumentsB(Value value) {
		return argument(value) && argumentsBTail(value);
	}

	private boolean argumentsBTail(Value value) {
		if (this.token.getType().equals(TokenType.ARGS_SEPARATOR)) {
			nextToken();

			if (argument(value)) {
				argumentsBTail(value);
			}
		}

		return true;
	}
}