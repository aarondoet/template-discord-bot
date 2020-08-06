package de.l0c4lh057.templatebot.commands;

import reactor.util.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;

public class ArgumentList extends ArrayList<String> {
	
	private final static ArgumentList empty = new ArgumentList();
	
	private int index = 0;
	private int filteredSize = -1;
	
	private static boolean isEmptyArgument(@NonNull String argument){
		return argument.length() == 0 || (argument.length() == 1 && !(Character.isWhitespace(argument.charAt(0)) && argument.equals(" ")));
	}
	
	private ArgumentList(){
		super();
	}
	
	private ArgumentList(@NonNull Collection<String> c){
		super(c);
	}
	
	public static ArgumentList empty(){ return empty; }
	
	/**
	 *
	 * @param content
	 * @return
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
	 *
	 * @return
	 */
	public boolean hasNext(){
		return hasNext(true);
	}
	
	/**
	 *
	 * @param skipEmpty
	 * @return
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
	 *
	 * @return
	 */
	@NonNull
	public String getNext(){
		return getNext(true, true);
	}
	
	/**
	 *
	 * @param increment
	 * @return
	 */
	@NonNull
	public String getNext(boolean increment){
		return getNext(true, increment);
	}
	
	/**
	 *
	 * @param skipEmpty
	 * @param increment
	 * @return
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
	 *
	 * @return
	 */
	@NonNull
	public String getCurrent(){
		return get(index - 1);
	}
	
	/**
	 *
	 * @return
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
	 *
	 * @return
	 */
	public int getFilteredSize(){
		if(filteredSize != -1) return filteredSize;
		filteredSize = (int)stream().filter(arg -> !isEmptyArgument(arg)).count();
		return filteredSize;
	}
	
	/**
	 *
	 * @return
	 */
	public int getCurrentIndex(){
		return index;
	}
	
	/**
	 *
	 * @param index
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
