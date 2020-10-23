package de.l0c4lh057.templatebot.commands;

import discord4j.common.util.Snowflake;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import discord4j.discordjson.json.EmbedData;
import discord4j.discordjson.json.MessageCreateRequest;
import discord4j.discordjson.json.MessageData;
import discord4j.rest.entity.RestChannel;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.Set;

public class Context {
	
	private final MessageCreateEvent event;
	
	private Context(MessageCreateEvent event){
		this.event = event;
	}
	
	public static Context ofEvent(MessageCreateEvent event){
		return new Context(event);
	}
	
	public MessageCreateEvent getEvent(){
		return event;
	}
	public Optional<Member> getMember(){
		return event.getMember();
	}
	public RestChannel getChannel(){
		return event.getMessage().getRestChannel();
	}
	public Message getMessage(){
		return event.getMessage();
	}
	public Optional<Snowflake> getGuildId(){
		return event.getGuildId();
	}
	public Snowflake getChannelId(){
		return event.getMessage().getChannelId();
	}
	public User getAuthor(){
		return event.getMessage().getAuthor().orElseThrow();
	}
	public String getMessageContent(){
		return event.getMessage().getContent();
	}
	public Set<Snowflake> getUserMentionIds(){
		return event.getMessage().getUserMentionIds();
	}
	public Set<Snowflake> getRoleMentionIds(){
		return event.getMessage().getRoleMentionIds();
	}
	public boolean mentionsUser(Snowflake userId){
		return event.getMessage().getUserMentionIds().contains(userId);
	}
	public boolean mentionsRole(Snowflake roleId){
		return event.getMessage().getRoleMentionIds().contains(roleId);
	}
	public boolean isGuildMessage(){
		return event.getGuildId().isPresent();
	}
	public boolean isPrivateMessage(){
		return event.getGuildId().isEmpty();
	}
	public GatewayDiscordClient getClient(){
		return event.getClient();
	}
	
	public Mono<MessageData> respond(EmbedData embedData){
		return event.getMessage().getRestChannel().createMessage(embedData);
	}
	public Mono<MessageData> respond(String content){
		return event.getMessage().getRestChannel().createMessage(content);
	}
	public Mono<MessageData> respond(MessageCreateRequest request){
		return event.getMessage().getRestChannel().createMessage(request);
	}
	
}
