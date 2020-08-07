package de.l0c4lh057.templatebot.commands;

import reactor.util.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;

public class ArgumentList extends ArrayList<String> {
	
	private int index = 0;
	private int filteredSize = -1;
	
	private static boolean isEmptyArgument(@NonNull String argument){
		return argument.length() == 0 || (argument.length() == 1 && Character.isWhitespace(argument.charAt(0)) && !argument.equals(" "));
	}
	
	private ArgumentList(){
		super();
	}
	
	private ArgumentList(@NonNull Collection<String> c){
		super(c);
	}
	
	/**
	 * @return An empty {@link ArgumentList} instance
	 */
	public static ArgumentList empty(){ return new ArgumentList(); }
	
	/**
	 * TODO add description here
	 *
	 * @param content The input that should get parsed into a list of arguments
	 * @return A new {@link ArgumentList} containing all the arguments parsed from the input
	 */
	@NonNull
	public static ArgumentList of(@NonNull String content){
		ArgumentList args = new ArgumentList();
		if(content.length() == 0) return args;
		boolean escaped = false;
		boolean inQuotes = false;
		char quoteChar = '-';
		boolean endedQuote = false;
		StringBuilder currArg = new StringBuilder();
		for (char c : content.toCharArray()) {
			if (endedQuote) {
				endedQuote = false;
				if(Character.isWhitespace(c)){
					if(c != ' ') args.add(""+c);
					quoteChar = '-';
					continue;
				}else{
					currArg = new StringBuilder(args.get(args.size()-1)).append(quoteChar);
					args.remove(args.size()-1);
				}
				quoteChar = '-';
			}
			if (Character.isWhitespace(c) && !inQuotes && !escaped) {
				args.add(currArg.toString());
				currArg = new StringBuilder();
				escaped = false;
				if(c != ' ') args.add(""+c);
				continue;
			}
			if (c == quoteChar && c != '-' && !escaped) {
				args.add(currArg.toString());
				currArg = new StringBuilder();
				inQuotes = false;
				endedQuote = true;
				continue;
			}
			if (c == '\\' && !escaped) {
				escaped = true;
				continue;
			}
			if ((c == '"' || c == '\'') && !escaped && !inQuotes && currArg.length() == 0) {
				currArg = new StringBuilder();
				inQuotes = true;
				quoteChar = c;
				continue;
			}
			escaped = false;
			currArg.append(c);
		}
		if (!endedQuote && currArg.length() > 0) args.add((inQuotes ? quoteChar : "") + currArg.toString());
		return args;
	}
	
	/**
	 * @return Whether there is a next argument in this list if empty arguments are skipped
	 */
	public boolean hasNext(){
		return hasNext(true);
	}
	
	/**
	 * @param skipEmpty If set to {@code true}, empty arguments will be skipped
	 * @return Whether there is a next argument in this list
	 */
	public boolean hasNext(boolean skipEmpty){
		if(!skipEmpty) return index < size();
		int j = index;
		while(j < size() && isEmptyArgument(get(j))){
			j++;
		}
		return j < size();
	}
	
	/**
	 * This function skips empty arguments and increments the index, which means that the next call of this function
	 * will return the argument after the one returned in the current call.
	 *
	 * @return The next argument
	 */
	@NonNull
	public String getNext(){
		return getNext(true, true);
	}
	
	/**
	 * This function skips empty arguments.
	 *
	 * @param increment If set to {@code true}, the index of the current argument will be increased. Otherwise it will
	 *                  just return the next argument and not increase the index, meaning that the next call of a
	 *                  {@code getNext} function will return the same argument again.
	 * @return The next argument
	 */
	@NonNull
	public String getNext(boolean increment){
		return getNext(true, increment);
	}
	
	/**
	 *
	 *
	 * @param skipEmpty Whether empty arguments should get skipped. An argument is considered empty if it has the length
	 *                  0 and they typically appear when a user auto completes emojis/mentions where a space is inserted
	 *                  afterwards and then presses the space key again.
	 * @param increment If set to {@code true}, the index of the current argument will be increased. Otherwise it will
	 *                  just return the next argument and not increase the index, meaning that the next call of a
	 *                  {@code getNext} function will return the same argument again.
	 * @return The next argument
	 */
	@NonNull
	public String getNext(boolean skipEmpty, boolean increment){
		if(!skipEmpty) return get(increment ? index++ : index);
		int j = index;
		while(j < size() && isEmptyArgument(get(j))){
			j++;
		}
		index = j;
		return get(increment ? index++ : index);
	}
	
	/**
	 * @return The current argument
	 */
	@NonNull
	public String getCurrent(){
		return get(index - 1);
	}
	
	/**
	 * This function joins all remaining arguments with a space and returns the string created by that.
	 * <p>
	 * This is preferred over {@link String#join(CharSequence, Iterable)} because arguments are split on all whitespaces,
	 * not just spaces, and this function takes care of that.
	 *
	 * @return The remaining arguments all joined with a space inbetween
	 */
	@NonNull
	public String getRemaining(){
		StringBuilder sb = new StringBuilder();
		for(int i = index; i < size(); i++){
			String currArg = get(i);
			if(i > index){
				String prevArg = get(i - 1);
				if(currArg.length() != 1 || !Character.isWhitespace(currArg.charAt(0)) || currArg.equals(" ")){
					if(prevArg.length() != 1 || !Character.isWhitespace(prevArg.charAt(0)) || prevArg.equals(" ")){
						sb.append(" ");
					}
				}
			}
			sb.append(currArg);
		}
		index = size();
		return sb.toString();
	}
	
	/**
	 * @return The size of this list with all empty arguments removed.
	 */
	public int getFilteredSize(){
		if(filteredSize != -1) return filteredSize;
		filteredSize = (int)stream().filter(arg -> !isEmptyArgument(arg)).count();
		return filteredSize;
	}
	
	/**
	 * @return The index of the current argument
	 */
	public int getCurrentIndex(){
		return index;
	}
	
	/**
	 * @param index The new index you want to use as a starting point
	 */
	public void setCurrentIndex(int index){
		this.index = index;
	}
	
	/**
	 * Returns a view of the portion of this list between the specified {@code fromIndex}, inclusive, and {@code toIndex},
	 * exclusive. (If {@code fromIndex} and {@code toIndex} are equal, the returned list is empty.)
	 * <p>
	 * Other than lists returned by {@link ArrayList#subList(int, int)}, the returned list is <b>NOT</b> backed by this
	 * list, so non-structural changes in the returned list are NOT reflected in this list.
	 *
	 * @param fromIndex low endpoint (inclusive) of the subList
	 * @param toIndex   high endpoint (exclusive) of the subList
	 * @return a copy of the specified range of the list
	 */
	@NonNull
	@Override
	public ArgumentList subList(int fromIndex, int toIndex) {
		return new ArgumentList(super.subList(fromIndex, toIndex));
	}
	
}
