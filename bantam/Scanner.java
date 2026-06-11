
package bantam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static bantam.TokenType.*; //not conventional

// no relationship to Java's built-in java.util.Scanner class other than sharing the same name.
class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);    
        keywords.put("if", IF);   
        keywords.put("nil", NIL); 
        keywords.put("or", OR);
        keywords.put("print",  PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",  SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while",  WHILE);
    }

    private int start = 0;
    private int current = 0;
    private int line = 1;

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme
            start = current;
            scanToken();
        }
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {
        case '(': addToken(LEFT_PAREN); break;
        case ')': addToken(RIGHT_PAREN); break;
        case '{': addToken(LEFT_BRACE); break;
        case '}': addToken(RIGHT_BRACE); break;
        case ',': addToken(COMMA); break;
        case '.': addToken(DOT); break;
        case '-': addToken(MINUS); break;
        case '+': addToken(PLUS); break;
        case ';': addToken(SEMICOLON); break;
        case '*': addToken(STAR); break;
        case '!':
            addToken(match('=') ? BANG_EQUAL : BANG);
            break;
        case '=':
            addToken(match('=') ? EQUAL_EQUAL : EQUAL);
            break;
        case '<':
            addToken(match('=') ? LESS_EQUAL : LESS);
            break;
        case '>':
            addToken(match('=') ? GREATER_EQUAL : GREATER);
            break;
        case '/':
            if (match('/')) {
                // A comment goes until the end of the line.
                //  we don’t end the token yet. Instead, we keep consuming characters until we reach the end of the line
                // when we reach the end of the comment, we don’t call addToken(); comment’s lexeme "disappears"
                while (peek() != '\n' && isAtEnd()) advance();
            } else {
                addToken(SLASH);
            }
            break;
        
        case ' ':
        case '\r':
        case '\t':
            // Ignore whitespace.
            break;
        case '\n':
            // This is why we used peek() to find the newline ending a comment instead of match()
            // we wanted to handle incrementing the line counter when faced with a new line
            line++;
            break;
        case '"': string(); break;
        
        // gives individual errors but a "combined error report" would be better
        // imagine fixing one error then the next springs up just after
        // Since hadError gets set, we’ll never try to execute any of the code, even though we keep going and scan the rest of it
        default:
            if (isDigit(c)) {
                number();
            // we begin by assuming any lexeme starting with a letter or underscore is an identifier otherwise it will be a keyword
            // if when the identifier is found in the lookup. Remember a reserved word/keyword IS also an identifier
            // also remember they should start with an alphabetical letter hence isAlpha() first and then under identifier we account 
            // for the numeric till the end of the identifier
            } else if (isAlpha()) {
                identifier();
            } else {
                Bantam.error(line, "Unexpected character");
            }
            break;
     }     
  }

    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        TokenType type = keywords.get(text);
        if (type == null) type = IDENTIFIER;
        addToken(type);
    }

    private void number() {
        while ((isDigit(peek()))) advance();

        // look for a fractional part
        if (peek() == '.' && isDigit(peekNext())) {
            // consume the "." now that we are assured a digit follows after it
            advance();

            while (isDigit(peek())) advance();
        }
        // convert the lexeme to its numeric value
        // interpreter uses Java’s Double type to represent numbers, so we produce a value of that type.
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));;
    }

    private void string() {
        // X: what about single quotes?
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }
        
        if (isAtEnd()) {
            Bantam.error(line, "Unterminated string");
            return;
        }
        
        // the closing "
        advance();

        // trim the surrounding quotes
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }


    private boolean match(char expected) {
        if(isAtEnd()) return false;
        if (source.charAt(expected) != expected) return false;

        // second advance after first of one of likely "two-character lexeme"
        // we only advance further, treating it as one lexeme from two advances if the after the first advance, the next is what we expect from one "two-character" lexeme"
        current++;
        return true;
    }
    
    // it doesn't consume the character, it's simply a "one character lookahead"
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 > source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
           (c>= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    // method overloading
    // shortcut to avoid typing in type "null" for the case of single characters
    // the "autofill method"
    private void addToken(TokenType type) {
        addToken(type, null);
    }

    // does the actual "adding token to list" work
    // If you ever need to change how tokens are created in the future, you only have to edit this single, longer method
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }


}