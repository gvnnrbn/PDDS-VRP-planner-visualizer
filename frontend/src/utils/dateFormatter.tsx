// ➕ Formateador reutilizable
export const formatDateTime = (fecha: Date) => {
    const dd = String(fecha.getDate()).padStart(2, "0");
    const mm = String(fecha.getMonth() + 1).padStart(2, "0");
    const yyyy = fecha.getFullYear();
    const hh = String(fecha.getHours()).padStart(2, "0");
    const min = String(fecha.getMinutes()).padStart(2, "0");
    const ss = String(fecha.getSeconds()).padStart(2, "0");
    return `${dd}/${mm}/${yyyy} ${hh}:${min}:${ss}`;
};