package domain;

import utils.Time;

public record Order(int id, int amountGLP, Position position, Time deadline){}
