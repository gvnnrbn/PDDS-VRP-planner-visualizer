package domain;

public record Order(int id, int amountGLP, Position position, Time deadline){}
