# Frontend

## Project Structure

```
src/
├── components/     # Reusable UI components
│   ├── Navbar.tsx # Main navigation
│   └── SectionBar.tsx # Section navigation
├── core/          # Core application logic
│   ├── services/  # API services
│   └── types/     # TypeScript type definitions
├── pages/         # Page components
│   ├── collapse-simulation/
│   ├── daily-operation/
│   └── weekly-simulation/
├── assets/        # Static assets
├── App.tsx        # Root component
└── main.tsx       # Entry point
```

## Core Architecture

### 1. Service Layer
The application uses a service-based architecture with a base service class that handles HTTP requests:

```typescript
// src/core/services/BaseService.ts
export class BaseService {
    protected async get<T>(endpoint: string): Promise<T>
    protected async put<T, R>(endpoint: string, data: T): Promise<R>
    protected async post<T, R>(endpoint: string, data: T): Promise<R>
    protected async delete(endpoint: string): Promise<void>
}
```

To create a new service:
1. Extend `BaseService`
2. Define type-safe methods for each API endpoint
3. Use TypeScript generics for type safety

Example:
```typescript
export class VehiculoService extends BaseService {
    async getAllVehiculos(): Promise<Vehiculo[]> {
        return this.get<Vehiculo[]>('/api/vehiculos')
    }
}
```

### 2. Type System
All API models are defined in `src/core/types/`:

```typescript
// src/core/types/vehiculo.ts
export type TipoVehiculo = 'TA' | 'TB' | 'TC' | 'TD';

export interface Vehiculo {
    id: number;
    tipo: TipoVehiculo;
    peso: number;
    maxCombustible: number;
    maxGlp: number;
    currCombustible: number;
    currGlp: number;
    posicionX: number;
    posicionY: number;
    disponible: boolean;
}
```

### 3. Component Structure
Components follow a functional React pattern with TypeScript:
- Use functional components with hooks
- Define props interfaces
- Use TypeScript for type safety
- Keep components small and focused

### 4. API Integration
The application uses Axios for HTTP requests:
- Base URL is configured via environment variable `VITE_API_URL`
- Default to `http://localhost:8080` if not set
- All requests are type-safe through the service layer

## Development Guidelines

### Adding New Features
1. Create new types in `src/core/types/`
2. Create new service in `src/core/services/`
3. Add new components in `src/components/` or `src/pages/`
4. Update existing components as needed

### Type Safety
- Always use TypeScript interfaces for props
- Use type guards when needed
- Keep types consistent with backend models

### Error Handling
- Use try/catch blocks for async operations
- Handle API errors gracefully
- Show user-friendly error messages

### State Management
- Use React hooks (useState, useEffect)
- Keep component state minimal
- Use context for global state if needed

## Page Structure
The application has three main pages:
- `collapse-simulation/`: For collapse simulation functionality
- `daily-operation/`: For daily operations
- `weekly-simulation/`: For weekly simulation features

Each page follows the same structure:
- Single entry component
- Uses services for data fetching
- Implements its own state management
- Reuses common components from `src/components/`

## Environment Setup

1. Install dependencies:
```bash
npm install
```

2. Set environment variables:
- `VITE_API_URL`: Backend API URL (defaults to http://localhost:8080)

3. Run the development server:
```bash
npm run dev
```
